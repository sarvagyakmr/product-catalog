"""
Redis ML-Integrated Subscriber

This module integrates the Redis subscriber with the background ML model manager.
It demonstrates non-blocking event processing with periodic model updates.

Features:
- Listens to Redis order events
- Feeds events to ML model manager (non-blocking)
- Periodic model retraining in background
- Real-time prediction API via command interface
- Statistics reporting
"""

import json
import logging
import sys
import os
import threading
import time
from typing import Dict, Any, Optional
from datetime import datetime

import redis

from order_repository import OrderRepository
from ml_model_manager import BackgroundMLModelManager, create_ml_integration

# Configure logging
logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s - %(name)s - %(levelname)s - %(message)s",
    handlers=[logging.StreamHandler(sys.stdout)]
)
logger = logging.getLogger(__name__)

# Configuration
DEFAULT_REDIS_HOST = "localhost"
DEFAULT_REDIS_PORT = 6379
DEFAULT_REDIS_DB = 0
DEFAULT_DB_PATH = "orders_history.db"

CHANNEL_ALLOCATED_ORDERS = "allocated-orders"
CHANNEL_CANCELLED_ORDERS = "cancelled-orders"


class RedisMLSubscriber:
    """
    Integrated Redis subscriber with ML model management.
    
    This class combines:
    - Redis pub/sub event listening
    - SQLite persistence
    - Background ML model updates
    - Interactive command interface
    """
    
    def __init__(
        self,
        redis_host: str = DEFAULT_REDIS_HOST,
        redis_port: int = DEFAULT_REDIS_PORT,
        redis_db: int = DEFAULT_REDIS_DB,
        db_path: str = DEFAULT_DB_PATH,
        ml_update_interval: int = 60
    ):
        self.redis_host = redis_host
        self.redis_port = redis_port
        self.redis_db = redis_db
        self.db_path = db_path
        
        # Components
        self.redis_client: Optional[redis.Redis] = None
        self.pubsub = None
        self.repository: Optional[OrderRepository] = None
        self.ml_manager: Optional[BackgroundMLModelManager] = None
        
        # State
        self._shutdown = threading.Event()
        self._command_thread: Optional[threading.Thread] = None
        self._stats_thread: Optional[threading.Thread] = None
        
        # ML Configuration
        self.ml_update_interval = ml_update_interval
        
        logger.info("RedisMLSubscriber initialized")
    
    def start(self) -> None:
        """Start all components."""
        logger.info("Starting Redis ML Subscriber...")
        
        # Initialize SQLite repository
        try:
            self.repository = OrderRepository(self.db_path)
            logger.info("SQLite repository initialized: %s", os.path.abspath(self.db_path))
        except Exception as e:
            logger.error("Failed to initialize repository: %s", e)
            raise
        
        # Initialize and start ML model manager
        self.ml_manager = BackgroundMLModelManager(
            db_path=self.db_path,
            update_interval_seconds=self.ml_update_interval
        )
        self.ml_manager.start()
        
        # Connect to Redis
        try:
            self.redis_client = redis.Redis(
                host=self.redis_host,
                port=self.redis_port,
                db=self.redis_db,
                decode_responses=False,
                socket_connect_timeout=10,
                socket_timeout=None
            )
            self.redis_client.ping()
            logger.info("Connected to Redis at %s:%s", self.redis_host, self.redis_port)
        except redis.ConnectionError as e:
            logger.error("Failed to connect to Redis: %s", e)
            self.ml_manager.stop()
            raise
        
        # Start command interface thread
        self._command_thread = threading.Thread(
            target=self._command_interface,
            name="CommandInterface",
            daemon=True
        )
        self._command_thread.start()
        
        # Start stats reporting thread
        self._stats_thread = threading.Thread(
            target=self._periodic_stats,
            name="StatsReporter",
            daemon=True
        )
        self._stats_thread.start()
        
        # Start listening for events
        self._listen_for_events()
    
    def stop(self) -> None:
        """Stop all components gracefully."""
        logger.info("Stopping Redis ML Subscriber...")
        self._shutdown.set()
        
        if self.ml_manager:
            self.ml_manager.stop()
        
        if self.pubsub:
            self.pubsub.close()
        
        if self.redis_client:
            self.redis_client.close()
        
        logger.info("Redis ML Subscriber stopped")
    
    def _listen_for_events(self) -> None:
        """Main event loop - listens to Redis and processes events."""
        self.pubsub = self.redis_client.pubsub()
        
        # Subscribe to channels
        channels = [CHANNEL_ALLOCATED_ORDERS, CHANNEL_CANCELLED_ORDERS]
        for channel in channels:
            self.pubsub.subscribe(channel)
            logger.info("Subscribed to channel: %s", channel)
        
        logger.info("Listening for events... (Press Ctrl+C to stop)")
        logger.info("Type 'help' in console for available commands")
        logger.info("")
        
        try:
            for message in self.pubsub.listen():
                if self._shutdown.is_set():
                    break
                
                if message["type"] == "message":
                    self._process_message(message)
                    
        except KeyboardInterrupt:
            logger.info("\nShutdown requested...")
        except Exception as e:
            logger.error("Error in event loop: %s", e)
        finally:
            self.stop()
    
    def _process_message(self, message: Dict[str, Any]) -> None:
        """
        Process a Redis message (non-blocking).
        
        This method quickly handles the event and delegates
        heavy processing to background threads.
        """
        try:
            # Decode message
            channel = message["channel"]
            if isinstance(channel, bytes):
                channel = channel.decode("utf-8")
            
            data = message["data"]
            if isinstance(data, bytes):
                data = data.decode("utf-8")
            
            # Parse JSON
            order_data = json.loads(data)
            order_id = order_data.get("id", "unknown")
            
            logger.debug("Received order %s on channel %s", order_id, channel)
            
            # Step 1: Persist to SQLite (fast, local write)
            if channel == CHANNEL_ALLOCATED_ORDERS and self.repository:
                try:
                    saved = self.repository.save_order_items(order_data)
                    logger.debug("Persisted %d items for order %s", saved, order_id)
                except Exception as e:
                    logger.error("Failed to persist order %s: %s", order_id, e)
            
            # Step 2: Notify ML manager (non-blocking queue)
            if self.ml_manager and channel == CHANNEL_ALLOCATED_ORDERS:
                self.ml_manager.on_new_order_event(order_data)
            
            # Step 3: Log to console (immediate feedback)
            self._log_order_event(channel, order_data)
            
        except json.JSONDecodeError as e:
            logger.error("Failed to parse JSON: %s", e)
        except Exception as e:
            logger.error("Error processing message: %s", e)
    
    def _log_order_event(self, channel: str, order_data: Dict[str, Any]) -> None:
        """Log order event details to console."""
        order_id = order_data.get("id", "N/A")
        channel_order_id = order_data.get("channelOrderId", "N/A")
        items = order_data.get("items", [])
        
        if channel == CHANNEL_ALLOCATED_ORDERS:
            logger.info("📦 ALLOCATED Order %s (%s) with %d items", 
                       order_id, channel_order_id, len(items))
        elif channel == CHANNEL_CANCELLED_ORDERS:
            logger.info("❌ CANCELLED Order %s (%s)", order_id, channel_order_id)
        
        # Log items
        for item in items:
            logger.info("   └─ Product %s: qty=%s", 
                       item.get("productId"), 
                       item.get("allocatedQuantity") or item.get("orderedQuantity"))
    
    def _command_interface(self) -> None:
        """
        Interactive command interface running in background thread.
        
        Available commands:
        - predict <product_id> [warehouse_id] [current_stock]
        - stats
        - report [warehouse_id]
        - cache
        - retrain
        - help
        - quit
        """
        # Wait a bit for startup to complete
        time.sleep(2)
        
        while not self._shutdown.is_set():
            try:
                # Non-blocking input check would be complex, so we use a prompt
                command = input("\n[ML-Subscriber] Enter command (or 'help'): ").strip()
                
                if not command:
                    continue
                
                parts = command.split()
                cmd = parts[0].lower()
                args = parts[1:]
                
                self._execute_command(cmd, args)
                
            except EOFError:
                # Handle piped input or Ctrl+D
                time.sleep(1)
            except Exception as e:
                logger.error("Command error: %s", e)
    
    def _execute_command(self, cmd: str, args: list) -> None:
        """Execute a command."""
        if cmd == "help":
            self._print_help()
        
        elif cmd == "predict" and len(args) >= 1:
            try:
                product_id = int(args[0])
                warehouse_id = int(args[1]) if len(args) >= 2 else 1
                current_stock = int(args[2]) if len(args) >= 3 else 0
                
                self._cmd_predict(product_id, warehouse_id, current_stock)
            except ValueError:
                logger.error("Invalid arguments. Usage: predict <product_id> [warehouse_id] [stock]")
        
        elif cmd == "stats":
            self._cmd_stats()
        
        elif cmd == "report":
            warehouse_id = int(args[0]) if args else 1
            self._cmd_report(warehouse_id)
        
        elif cmd == "cache":
            self._cmd_cache_stats()
        
        elif cmd == "retrain":
            self._cmd_retrain()
        
        elif cmd == "batch":
            self._cmd_batch_predict(args)
        
        elif cmd == "quit" or cmd == "exit":
            logger.info("Shutdown requested via command")
            self._shutdown.set()
            raise SystemExit
        
        else:
            logger.warning("Unknown command: %s. Type 'help' for available commands.", cmd)
    
    def _print_help(self) -> None:
        """Print help message."""
        help_text = """
Available Commands:
  predict <product_id> [warehouse_id] [stock]
      Get restock prediction for a product
      Example: predict 42 1 50

  stats
      Show ML model manager statistics

  report [warehouse_id]
      Generate restock report for warehouse
      Example: report 1

  cache
      Show prediction cache statistics

  retrain
      Force immediate model retraining

  batch <product_id> [<product_id> ...]
      Get predictions for multiple products
      Example: batch 1 2 3 4 5

  help
      Show this help message

  quit / exit
      Stop the subscriber
        """
        print(help_text)
    
    def _cmd_predict(self, product_id: int, warehouse_id: int, current_stock: int) -> None:
        """Handle predict command."""
        if not self.ml_manager:
            logger.error("ML manager not available")
            return
        
        logger.info("Getting prediction for product %s...", product_id)
        
        prediction = self.ml_manager.get_restock_prediction(
            product_id=product_id,
            warehouse_id=warehouse_id,
            current_stock=current_stock
        )
        
        if prediction:
            print("\n" + "=" * 50)
            print(f"RESTOCK PREDICTION for Product {product_id}")
            print("=" * 50)
            print(f"Recommended Quantity: {prediction.recommended_quantity} units")
            print(f"Average Daily Demand: {prediction.avg_daily_demand:.2f}")
            print(f"Demand Volatility: {prediction.demand_volatility:.2f}")
            print(f"Days of Stock: {prediction.days_of_stock}")
            print(f"Confidence Score: {prediction.confidence_score:.2f}")
            print(f"Method: {prediction.prediction_method}")
            print("=" * 50)
        else:
            logger.warning("No prediction available for product %s", product_id)
    
    def _cmd_stats(self) -> None:
        """Handle stats command."""
        if not self.ml_manager:
            logger.error("ML manager not available")
            return
        
        stats = self.ml_manager.get_stats()
        
        print("\n" + "=" * 50)
        print("ML MODEL MANAGER STATISTICS")
        print("=" * 50)
        print(f"Events Received: {stats['events_received']}")
        print(f"Events Processed: {stats['events_processed']}")
        print(f"Model Updates: {stats['model_updates']}")
        print(f"Predictions Served: {stats['predictions_served']}")
        print(f"Cache Hits: {stats['cache_hits']}")
        print(f"Cache Misses: {stats['cache_misses']}")
        print(f"Current Queue Size: {stats['queue_size']}")
        print(f"Cache Size: {stats['cache_size']}")
        print(f"Model Ready: {stats['model_ready']}")
        print(f"Last Update: {stats['last_update']}")
        print("=" * 50)
    
    def _cmd_report(self, warehouse_id: int) -> None:
        """Handle report command."""
        if not self.repository or not self.ml_manager:
            logger.error("Components not available")
            return
        
        from restock_predictor import RestockAnalyzer
        
        logger.info("Generating restock report for warehouse %s...", warehouse_id)
        
        try:
            analyzer = RestockAnalyzer(self.db_path)
            report = analyzer.generate_restock_report(warehouse_id)
            
            print("\n" + "=" * 60)
            print(f"RESTOCK REPORT - Warehouse {warehouse_id}")
            print("=" * 60)
            print(f"Generated: {report['generated_at']}")
            print(f"\nSummary:")
            print(f"  Products Analyzed: {report['summary']['total_products_analyzed']}")
            print(f"  Needing Restock: {report['summary']['products_needing_restock']}")
            print(f"  Total Units: {report['summary']['total_units_to_restock']}")
            
            if report['recommendations']:
                print(f"\nTop 5 Recommendations:")
                for i, rec in enumerate(report['recommendations'][:5], 1):
                    print(f"  {i}. Product {rec['product_id']}: "
                          f"{rec['recommended_quantity']} units "
                          f"(confidence: {rec['confidence_score']:.2f})")
            
            print("=" * 60)
            
        except Exception as e:
            logger.error("Failed to generate report: %s", e)
    
    def _cmd_cache_stats(self) -> None:
        """Handle cache command."""
        if not self.ml_manager:
            logger.error("ML manager not available")
            return
        
        stats = self.ml_manager.get_stats()
        total_requests = stats['cache_hits'] + stats['cache_misses']
        hit_rate = (stats['cache_hits'] / total_requests * 100) if total_requests > 0 else 0
        
        print("\n" + "=" * 50)
        print("PREDICTION CACHE STATISTICS")
        print("=" * 50)
        print(f"Cache Size: {stats['cache_size']} entries")
        print(f"Cache Hits: {stats['cache_hits']}")
        print(f"Cache Misses: {stats['cache_misses']}")
        print(f"Hit Rate: {hit_rate:.1f}%")
        print("=" * 50)
    
    def _cmd_retrain(self) -> None:
        """Handle retrain command."""
        if not self.ml_manager:
            logger.error("ML manager not available")
            return
        
        logger.info("Triggering model retrain...")
        self.ml_manager.force_retrain()
        print("Retrain queued. Model will update shortly.")
    
    def _cmd_batch_predict(self, product_ids: list) -> None:
        """Handle batch prediction command."""
        if not self.ml_manager:
            logger.error("ML manager not available")
            return
        
        try:
            ids = [int(pid) for pid in product_ids]
            results = self.ml_manager.get_batch_predictions(ids, warehouse_id=1)
            
            print("\n" + "=" * 60)
            print("BATCH PREDICTIONS")
            print("=" * 60)
            print(f"{'Product':<12} {'Qty':<10} {'Confidence':<12} {'Days Stock':<12}")
            print("-" * 60)
            
            for pid in ids:
                pred = results.get(pid)
                if pred:
                    print(f"{pid:<12} {pred.recommended_quantity:<10} "
                          f"{pred.confidence_score:<12.2f} {pred.days_of_stock:<12}")
                else:
                    print(f"{pid:<12} {'N/A':<10} {'N/A':<12} {'N/A':<12}")
            
            print("=" * 60)
            
        except ValueError:
            logger.error("Invalid product IDs. Usage: batch <id1> <id2> ...")
    
    def _periodic_stats(self) -> None:
        """Periodically log statistics."""
        while not self._shutdown.is_set():
            time.sleep(300)  # Every 5 minutes
            
            if self._shutdown.is_set():
                break
            
            try:
                if self.ml_manager:
                    stats = self.ml_manager.get_stats()
                    logger.info("[Stats] Events: %d processed, %d queued | "
                               "Cache: %d entries, %d hits | "
                               "Updates: %d",
                               stats['events_processed'],
                               stats['queue_size'],
                               stats['cache_size'],
                               stats['cache_hits'],
                               stats['model_updates'])
            except Exception as e:
                logger.debug("Stats error: %s", e)


def main():
    """Main entry point."""
    import argparse
    
    parser = argparse.ArgumentParser(
        description="Redis ML-Integrated Subscriber with Background Model Updates"
    )
    parser.add_argument("--host", default=DEFAULT_REDIS_HOST, help="Redis host")
    parser.add_argument("--port", type=int, default=DEFAULT_REDIS_PORT, help="Redis port")
    parser.add_argument("--db-path", default=DEFAULT_DB_PATH, help="SQLite database path")
    parser.add_argument("--ml-interval", type=int, default=60, 
                       help="ML model update interval (seconds)")
    
    args = parser.parse_args()
    
    subscriber = RedisMLSubscriber(
        redis_host=args.host,
        redis_port=args.port,
        db_path=args.db_path,
        ml_update_interval=args.ml_interval
    )
    
    try:
        subscriber.start()
    except KeyboardInterrupt:
        logger.info("Interrupted by user")
    finally:
        subscriber.stop()


if __name__ == "__main__":
    main()
