"""
Redis Order Event Subscriber

This module connects to the embedded Redis instance and subscribes to order
creation/allocation events published by the Spring Boot Order Management Service.

Subscribed Channels:
- allocated-orders: Published when orders are fully allocated (inventory reserved)
- cancelled-orders: Published when orders are cancelled

Features:
- Persists order items to SQLite for historical analysis
- Integrates with ML-based restock prediction
"""

import json
import logging
import sys
import os
from typing import Dict, Any, Optional

import redis

from order_repository import OrderRepository
from restock_predictor import RestockAnalyzer

# Configure logging
logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s - %(name)s - %(levelname)s - %(message)s",
    handlers=[
        logging.StreamHandler(sys.stdout)
    ]
)
logger = logging.getLogger(__name__)

# Redis Configuration
DEFAULT_REDIS_HOST = "localhost"
DEFAULT_REDIS_PORT = 6379
DEFAULT_REDIS_DB = 0

# SQLite Configuration
DEFAULT_DB_PATH = "orders_history.db"

# Event channel names (matching EventNames.java in commons module)
CHANNEL_ALLOCATED_ORDERS = "allocated-orders"
CHANNEL_CANCELLED_ORDERS = "cancelled-orders"


def parse_order_items(order_data: Dict[str, Any]) -> Optional[list]:
    """
    Extract order items from the order data.
    
    The OutwardOrderDto structure contains:
    - id: Order ID
    - channelOrderId: External order reference
    - channel: Sales channel (e.g., "INTERNAL")
    - warehouseId: Warehouse ID
    - status: Order status (ALLOCATED, etc.)
    - items: List of order items (if included in the event)
    
    Args:
        order_data: Parsed JSON order data
        
    Returns:
        List of order items or None if not present
    """
    items = order_data.get("items")
    if items and isinstance(items, list):
        return items
    return None


def log_order_details(order_data: Dict[str, Any]) -> None:
    """
    Log order details and items to console.
    
    Args:
        order_data: Parsed JSON order data from Redis
    """
    order_id = order_data.get("id", "UNKNOWN")
    channel_order_id = order_data.get("channelOrderId", "N/A")
    channel = order_data.get("channel", "N/A")
    warehouse_id = order_data.get("warehouseId", "N/A")
    status = order_data.get("status", "N/A")
    
    # Log order header
    logger.info("=" * 60)
    logger.info("ORDER EVENT RECEIVED")
    logger.info("=" * 60)
    logger.info("Order ID: %s", order_id)
    logger.info("Channel Order ID: %s", channel_order_id)
    logger.info("Channel: %s", channel)
    logger.info("Warehouse ID: %s", warehouse_id)
    logger.info("Status: %s", status)
    
    # Log order items
    items = parse_order_items(order_data)
    if items:
        logger.info("-" * 60)
        logger.info("ORDER ITEMS (%d items):", len(items))
        logger.info("-" * 60)
        
        for idx, item in enumerate(items, 1):
            item_id = item.get("id", "N/A")
            product_id = item.get("productId", "N/A")
            ordered_qty = item.get("orderedQuantity", "N/A")
            allocated_qty = item.get("allocatedQuantity", "N/A")
            
            logger.info(
                "  Item #%d: productId=%s, orderedQuantity=%s, allocatedQuantity=%s (itemId=%s)",
                idx, product_id, ordered_qty, allocated_qty, item_id
            )
    else:
        logger.info("No order items included in this event")
    
    logger.info("=" * 60)
    logger.info("")  # Empty line for readability


def handle_message(
    message: Dict[str, Any], 
    repository: Optional[OrderRepository] = None
) -> None:
    """
    Handle incoming Redis pub/sub message.
    
    Args:
        message: Redis message dictionary with 'type', 'channel', and 'data' keys
        repository: OrderRepository instance for persisting order data
    """
    try:
        channel = message.get("channel", b"").decode("utf-8") if isinstance(message.get("channel"), bytes) else message.get("channel", "")
        data = message.get("data", b"")
        
        # Decode bytes to string if necessary
        if isinstance(data, bytes):
            data = data.decode("utf-8")
        
        logger.debug("Received message on channel '%s': %s", channel, data)
        
        # Parse JSON payload
        order_data = json.loads(data)
        
        # Persist to SQLite if repository provided
        if repository and channel == CHANNEL_ALLOCATED_ORDERS:
            try:
                saved_count = repository.save_order_items(order_data)
                logger.debug("Persisted %d order items to SQLite", saved_count)
            except Exception as e:
                logger.error("Failed to persist order to database: %s", e)
        
        # Log based on channel type
        if channel == CHANNEL_ALLOCATED_ORDERS:
            logger.info("[ALLOCATED ORDER] Order has been allocated and is ready for fulfillment")
            log_order_details(order_data)
        elif channel == CHANNEL_CANCELLED_ORDERS:
            logger.info("[CANCELLED ORDER] Order has been cancelled")
            log_order_details(order_data)
        else:
            logger.warning("[UNKNOWN CHANNEL] Received message on channel: %s", channel)
            log_order_details(order_data)
            
    except json.JSONDecodeError as e:
        logger.error("Failed to parse JSON message: %s", e)
        logger.error("Raw message data: %s", data)
    except Exception as e:
        logger.error("Error processing message: %s", e, exc_info=True)


def subscribe_to_order_events(
    host: str = DEFAULT_REDIS_HOST,
    port: int = DEFAULT_REDIS_PORT,
    db: int = DEFAULT_REDIS_DB,
    channels: list = None,
    db_path: str = DEFAULT_DB_PATH,
    enable_persistence: bool = True
) -> None:
    """
    Subscribe to Redis channels and listen for order events.
    
    This function connects to the embedded Redis instance and blocks,
    listening for published order events. Order data is persisted to SQLite
    for historical analysis and ML-based restock prediction.
    
    Args:
        host: Redis host (default: localhost)
        port: Redis port (default: 6379)
        db: Redis database number (default: 0)
        channels: List of channels to subscribe to (default: allocated-orders, cancelled-orders)
        db_path: Path to SQLite database file
        enable_persistence: Whether to persist orders to SQLite
    """
    if channels is None:
        channels = [CHANNEL_ALLOCATED_ORDERS, CHANNEL_CANCELLED_ORDERS]
    
    # Initialize repository if persistence enabled
    repository = None
    if enable_persistence:
        try:
            repository = OrderRepository(db_path)
            logger.info("SQLite persistence enabled: %s", os.path.abspath(db_path))
        except Exception as e:
            logger.error("Failed to initialize SQLite repository: %s", e)
            logger.warning("Continuing without persistence")
    
    logger.info("Connecting to Redis at %s:%s (db=%s)...", host, port, db)
    
    try:
        # Create Redis connection
        redis_client = redis.Redis(
            host=host,
            port=port,
            db=db,
            decode_responses=False,  # We'll handle decoding manually
            socket_connect_timeout=10,
            socket_timeout=None  # Block indefinitely waiting for messages
        )
        
        # Test connection
        redis_client.ping()
        logger.info("Successfully connected to Redis")
        
        # Create pub/sub client
        pubsub = redis_client.pubsub()
        
        # Subscribe to channels
        for channel in channels:
            pubsub.subscribe(channel)
            logger.info("Subscribed to channel: %s", channel)
        
        logger.info("Listening for order events... (Press Ctrl+C to stop)")
        logger.info("")
        
        # Listen for messages (blocking)
        for message in pubsub.listen():
            # Skip subscription confirmation messages
            if message["type"] == "message":
                handle_message(message, repository)
            elif message["type"] == "subscribe":
                logger.debug("Subscribed to channel: %s", message["channel"])
                
    except redis.ConnectionError as e:
        logger.error("Failed to connect to Redis: %s", e)
        logger.error("Make sure the Spring Boot application with embedded Redis is running")
        sys.exit(1)
    except KeyboardInterrupt:
        logger.info("\nShutting down subscriber...")
        if 'pubsub' in locals():
            pubsub.close()
        if 'redis_client' in locals():
            redis_client.close()
        logger.info("Subscriber stopped")
        
        # Print summary if we have data
        if repository:
            try:
                summary = repository.get_summary_stats()
                logger.info("\n📊 Session Summary:")
                logger.info("  Total order items stored: %s", summary["total_order_items"])
                logger.info("  Unique orders: %s", summary["total_unique_orders"])
                logger.info("  Unique products: %s", summary["total_unique_products"])
                if summary["date_range"]["earliest"]:
                    logger.info("  Data range: %s to %s", 
                        summary["date_range"]["earliest"], 
                        summary["date_range"]["latest"])
            except Exception as e:
                logger.debug("Could not get summary stats: %s", e)
                
    except Exception as e:
        logger.error("Unexpected error: %s", e, exc_info=True)
        sys.exit(1)


def run_restock_analysis(db_path: str, warehouse_id: int = 1) -> None:
    """
    Run ML-based restock analysis on collected data.
    
    Args:
        db_path: Path to SQLite database
        warehouse_id: Warehouse to analyze
    """
    logger.info("\n" + "=" * 60)
    logger.info("ML RESTOCK ANALYSIS")
    logger.info("=" * 60)
    
    try:
        analyzer = RestockAnalyzer(db_path)
        
        # Generate comprehensive report
        report = analyzer.generate_restock_report(warehouse_id)
        
        logger.info("\n📈 Restock Report for Warehouse %s", warehouse_id)
        logger.info("Generated at: %s", report["generated_at"])
        logger.info("\nSummary:")
        logger.info("  Products analyzed: %s", report["summary"]["total_products_analyzed"])
        logger.info("  Products needing restock: %s", report["summary"]["products_needing_restock"])
        logger.info("  Total units to restock: %s", report["summary"]["total_units_to_restock"])
        
        if report["recommendations"]:
            logger.info("\n🔝 Top Restock Recommendations:")
            for i, rec in enumerate(report["recommendations"][:5], 1):
                logger.info(
                    "  %d. Product %s: %s units (confidence: %.2f, days of stock: %d)",
                    i,
                    rec["product_id"],
                    rec["recommended_quantity"],
                    rec["confidence_score"],
                    rec["days_of_stock"]
                )
        
        if report["low_stock_alerts"]:
            logger.info("\n🚨 Low Stock Alerts (≤3 days):")
            for alert in report["low_stock_alerts"][:5]:
                logger.info(
                    "  - Product %s: %s days of stock remaining",
                    alert["product_id"],
                    alert["days_of_stock"]
                )
        
        logger.info("=" * 60)
        
    except Exception as e:
        logger.error("Failed to run restock analysis: %s", e)


def main():
    """Main entry point for the Redis order subscriber."""
    import argparse
    
    parser = argparse.ArgumentParser(
        description="Subscribe to Redis order events from Spring Boot application"
    )
    parser.add_argument(
        "--host",
        default=DEFAULT_REDIS_HOST,
        help=f"Redis host (default: {DEFAULT_REDIS_HOST})"
    )
    parser.add_argument(
        "--port",
        type=int,
        default=DEFAULT_REDIS_PORT,
        help=f"Redis port (default: {DEFAULT_REDIS_PORT})"
    )
    parser.add_argument(
        "--channel",
        action="append",
        choices=[CHANNEL_ALLOCATED_ORDERS, CHANNEL_CANCELLED_ORDERS, "all"],
        default=None,
        help="Channel(s) to subscribe to (default: all order channels)"
    )
    parser.add_argument(
        "--db-path",
        default=DEFAULT_DB_PATH,
        help=f"SQLite database path (default: {DEFAULT_DB_PATH})"
    )
    parser.add_argument(
        "--no-persistence",
        action="store_true",
        help="Disable SQLite persistence"
    )
    parser.add_argument(
        "--analyze",
        action="store_true",
        help="Run restock analysis after subscriber stops"
    )
    parser.add_argument(
        "--warehouse-id",
        type=int,
        default=1,
        help="Warehouse ID for analysis (default: 1)"
    )
    
    args = parser.parse_args()
    
    # Determine channels to subscribe to
    if args.channel:
        if "all" in args.channel:
            channels = [CHANNEL_ALLOCATED_ORDERS, CHANNEL_CANCELLED_ORDERS]
        else:
            channels = args.channel
    else:
        channels = [CHANNEL_ALLOCATED_ORDERS, CHANNEL_CANCELLED_ORDERS]
    
    logger.info("Starting Redis Order Event Subscriber")
    logger.info("Target channels: %s", ", ".join(channels))
    
    subscribe_to_order_events(
        host=args.host,
        port=args.port,
        channels=channels,
        db_path=args.db_path,
        enable_persistence=not args.no_persistence
    )
    
    # Run analysis if requested
    if args.analyze and not args.no_persistence:
        run_restock_analysis(args.db_path, args.warehouse_id)


if __name__ == "__main__":
    main()
