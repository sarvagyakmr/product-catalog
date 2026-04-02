"""
ML Model Manager Module

Provides a thread-safe, non-blocking model management system that:
1. Buffers incoming order events in a queue
2. Periodically updates the restock prediction model in a background thread
3. Maintains model state and triggers retraining when thresholds are met
4. Provides real-time predictions without blocking the event listener
"""

import logging
import threading
import time
import queue
from typing import Dict, Any, List, Optional, Callable
from dataclasses import dataclass, field
from datetime import datetime, timedelta
from collections import defaultdict
import json

from order_repository import OrderRepository
from restock_predictor import MovingAverageRestockPredictor, RestockRecommendation

logger = logging.getLogger(__name__)


@dataclass
class ModelUpdateEvent:
    """Represents an event that triggers model update."""
    event_type: str  # 'new_order', 'scheduled_update', 'force_retrain'
    data: Dict[str, Any] = field(default_factory=dict)
    timestamp: datetime = field(default_factory=datetime.now)


@dataclass
class PredictionCache:
    """Cached prediction with timestamp for freshness checking."""
    prediction: RestockRecommendation
    timestamp: datetime
    
    def is_fresh(self, max_age_seconds: int = 300) -> bool:
        """Check if cached prediction is still fresh."""
        return (datetime.now() - self.timestamp).seconds < max_age_seconds


class BackgroundMLModelManager:
    """
    Thread-safe ML model manager that runs predictions in background.
    
    Features:
    - Event buffering with queue
    - Periodic model updates (non-blocking)
    - Prediction caching for fast lookups
    - Batch processing of accumulated events
    - Automatic retraining triggers
    """
    
    def __init__(
        self,
        db_path: str = "orders_history.db",
        update_interval_seconds: int = 60,
        batch_size: int = 100,
        min_events_for_update: int = 10,
        prediction_cache_ttl: int = 300
    ):
        """
        Initialize the background ML model manager.
        
        Args:
            db_path: Path to SQLite database
            update_interval_seconds: How often to run background updates
            batch_size: Max events to process in one batch
            min_events_for_update: Minimum events before triggering update
            prediction_cache_ttl: How long to cache predictions (seconds)
        """
        self.db_path = db_path
        self.update_interval = update_interval_seconds
        self.batch_size = batch_size
        self.min_events_for_update = min_events_for_update
        self.cache_ttl = prediction_cache_ttl
        
        # Thread-safe components
        self._event_queue: queue.Queue[ModelUpdateEvent] = queue.Queue()
        self._prediction_cache: Dict[tuple, PredictionCache] = {}
        self._cache_lock = threading.RLock()
        self._stats_lock = threading.RLock()
        
        # Model state
        self._repository: Optional[OrderRepository] = None
        self._predictor: Optional[MovingAverageRestockPredictor] = None
        self._model_ready = False
        self._last_update_time: Optional[datetime] = None
        
        # Statistics
        self._stats = {
            "events_received": 0,
            "events_processed": 0,
            "model_updates": 0,
            "predictions_served": 0,
            "cache_hits": 0,
            "cache_misses": 0
        }
        
        # Background thread
        self._worker_thread: Optional[threading.Thread] = None
        self._shutdown_event = threading.Event()
        
        # Event callbacks
        self._update_callbacks: List[Callable[[Dict[str, Any]], None]] = []
        
        logger.info("BackgroundMLModelManager initialized (update_interval=%ds)", 
                   update_interval_seconds)
    
    def start(self) -> None:
        """Start the background worker thread."""
        if self._worker_thread and self._worker_thread.is_alive():
            logger.warning("Model manager already running")
            return
        
        # Initialize database connection in main thread
        try:
            self._repository = OrderRepository(self.db_path)
            self._predictor = MovingAverageRestockPredictor(self._repository)
            self._model_ready = True
            logger.info("ML model initialized successfully")
        except Exception as e:
            logger.error("Failed to initialize ML model: %s", e)
            raise
        
        # Start background worker
        self._shutdown_event.clear()
        self._worker_thread = threading.Thread(
            target=self._background_worker,
            name="MLModelUpdater",
            daemon=True
        )
        self._worker_thread.start()
        logger.info("Background ML worker thread started")
    
    def stop(self) -> None:
        """Stop the background worker thread gracefully."""
        logger.info("Stopping background ML model manager...")
        self._shutdown_event.set()
        
        # Put a shutdown sentinel in the queue
        self._event_queue.put(ModelUpdateEvent(event_type="shutdown"))
        
        if self._worker_thread and self._worker_thread.is_alive():
            self._worker_thread.join(timeout=5.0)
            if self._worker_thread.is_alive():
                logger.warning("Worker thread did not stop gracefully")
        
        logger.info("Background ML model manager stopped")
    
    def _background_worker(self) -> None:
        """
        Background worker that processes events and updates model.
        
        This runs in a separate thread and never blocks the main event loop.
        """
        logger.info("Background worker started")
        last_scheduled_update = time.time()
        
        while not self._shutdown_event.is_set():
            try:
                # Wait for events with timeout to allow periodic updates
                timeout = max(1, self.update_interval - (time.time() - last_scheduled_update))
                
                try:
                    event = self._event_queue.get(timeout=timeout)
                    if event.event_type == "shutdown":
                        break
                    
                    # Process the event
                    self._process_single_event(event)
                    
                except queue.Empty:
                    # Timeout - check if scheduled update is due
                    pass
                
                # Check if scheduled update is due
                current_time = time.time()
                if current_time - last_scheduled_update >= self.update_interval:
                    self._run_scheduled_update()
                    last_scheduled_update = current_time
                
                # Process any batched events
                self._process_batch_if_needed()
                
            except Exception as e:
                logger.error("Error in background worker: %s", e, exc_info=True)
                time.sleep(1)  # Brief pause on error
        
        logger.info("Background worker exiting")
    
    def _process_single_event(self, event: ModelUpdateEvent) -> None:
        """Process a single event (called from background thread)."""
        with self._stats_lock:
            self._stats["events_processed"] += 1
        
        if event.event_type == "new_order":
            # Save order to database (already done by subscriber, but we track it)
            order_data = event.data.get("order_data", {})
            logger.debug("Background worker processed new order: %s", 
                        order_data.get("id", "unknown"))
            
            # Invalidate cache for affected products
            self._invalidate_cache_for_order(order_data)
    
    def _process_batch_if_needed(self) -> None:
        """Process batched events if queue has accumulated enough."""
        if self._event_queue.qsize() >= self.batch_size:
            logger.info("Processing batch of %d events", self.batch_size)
            processed = 0
            
            while processed < self.batch_size and not self._event_queue.empty():
                try:
                    event = self._event_queue.get_nowait()
                    if event.event_type != "shutdown":
                        self._process_single_event(event)
                        processed += 1
                except queue.Empty:
                    break
            
            # Trigger model update after batch processing
            self._trigger_model_update()
    
    def _run_scheduled_update(self) -> None:
        """Run periodic scheduled model update."""
        logger.debug("Running scheduled model update")
        
        if not self._model_ready:
            logger.warning("Model not ready, skipping update")
            return
        
        try:
            # Clear old cache entries
            self._clear_expired_cache()
            
            # Pre-compute predictions for popular products
            self._precompute_popular_predictions()
            
            with self._stats_lock:
                self._stats["model_updates"] += 1
            
            self._last_update_time = datetime.now()
            logger.info("Scheduled model update completed")
            
        except Exception as e:
            logger.error("Error during scheduled update: %s", e)
    
    def _trigger_model_update(self) -> None:
        """Trigger immediate model update (called after batch processing)."""
        logger.info("Triggering model update after batch processing")
        self._run_scheduled_update()
    
    def _invalidate_cache_for_order(self, order_data: Dict[str, Any]) -> None:
        """Invalidate cache entries for products in the order."""
        warehouse_id = order_data.get("warehouseId")
        items = order_data.get("items", [])
        
        with self._cache_lock:
            for item in items:
                product_id = item.get("productId")
                cache_key = (product_id, warehouse_id)
                if cache_key in self._prediction_cache:
                    del self._prediction_cache[cache_key]
                    logger.debug("Invalidated cache for product %s", product_id)
    
    def _clear_expired_cache(self) -> None:
        """Remove expired entries from prediction cache."""
        with self._cache_lock:
            expired_keys = [
                key for key, cache in self._prediction_cache.items()
                if not cache.is_fresh(self.cache_ttl)
            ]
            for key in expired_keys:
                del self._prediction_cache[key]
            
            if expired_keys:
                logger.debug("Cleared %d expired cache entries", len(expired_keys))
    
    def _precompute_popular_predictions(self, top_n: int = 20) -> None:
        """Pre-compute predictions for most popular products."""
        if not self._repository or not self._predictor:
            return
        
        try:
            # Get all products and their order frequency
            products = self._repository.get_all_products()
            
            # For now, pre-compute for all products (can be optimized)
            for product_id in products[:top_n]:
                # Default to warehouse 1 for pre-computation
                cache_key = (product_id, 1)
                
                try:
                    prediction = self._predictor.predict_restock(
                        product_id=product_id,
                        warehouse_id=1,
                        current_stock=0  # Assume zero for general prediction
                    )
                    
                    with self._cache_lock:
                        self._prediction_cache[cache_key] = PredictionCache(
                            prediction=prediction,
                            timestamp=datetime.now()
                        )
                except Exception as e:
                    logger.debug("Failed to precompute for product %s: %s", product_id, e)
            
            logger.debug("Pre-computed predictions for %d products", 
                        min(len(products), top_n))
            
        except Exception as e:
            logger.error("Error during pre-computation: %s", e)
    
    # ============ Public API (Thread-Safe) ============
    
    def on_new_order_event(self, order_data: Dict[str, Any]) -> None:
        """
        Called when a new order event is received (non-blocking).
        
        This is the main entry point from the Redis subscriber.
        It immediately returns and queues the event for background processing.
        
        Args:
            order_data: Parsed order data from Redis
        """
        event = ModelUpdateEvent(
            event_type="new_order",
            data={"order_data": order_data}
        )
        
        # Non-blocking queue put
        try:
            self._event_queue.put_nowait(event)
            with self._stats_lock:
                self._stats["events_received"] += 1
        except queue.Full:
            logger.warning("Event queue full, dropping event")
    
    def get_restock_prediction(
        self, 
        product_id: int, 
        warehouse_id: int = 1,
        current_stock: int = 0,
        use_cache: bool = True
    ) -> Optional[RestockRecommendation]:
        """
        Get restock prediction (fast, non-blocking).
        
        First checks cache, then falls back to real-time calculation
        if cache miss or stale.
        
        Args:
            product_id: Product to analyze
            warehouse_id: Warehouse ID
            current_stock: Current inventory level
            use_cache: Whether to use cached predictions
            
        Returns:
            RestockRecommendation or None if model not ready
        """
        if not self._model_ready or not self._predictor:
            logger.warning("Model not ready, cannot provide prediction")
            return None
        
        cache_key = (product_id, warehouse_id)
        
        # Check cache first
        if use_cache:
            with self._cache_lock:
                cached = self._prediction_cache.get(cache_key)
                if cached and cached.is_fresh(self.cache_ttl):
                    with self._stats_lock:
                        self._stats["cache_hits"] += 1
                    logger.debug("Cache hit for product %s", product_id)
                    return cached.prediction
        
        with self._stats_lock:
            self._stats["cache_misses"] += 1
        
        # Cache miss - calculate in real-time (this may be slower)
        try:
            prediction = self._predictor.predict_restock(
                product_id=product_id,
                warehouse_id=warehouse_id,
                current_stock=current_stock
            )
            
            # Update cache
            with self._cache_lock:
                self._prediction_cache[cache_key] = PredictionCache(
                    prediction=prediction,
                    timestamp=datetime.now()
                )
            
            with self._stats_lock:
                self._stats["predictions_served"] += 1
            
            return prediction
            
        except Exception as e:
            logger.error("Error calculating prediction: %s", e)
            return None
    
    def get_batch_predictions(
        self,
        product_ids: List[int],
        warehouse_id: int = 1,
        current_stock_map: Optional[Dict[int, int]] = None
    ) -> Dict[int, Optional[RestockRecommendation]]:
        """
        Get predictions for multiple products efficiently.
        
        Args:
            product_ids: List of product IDs
            warehouse_id: Warehouse ID
            current_stock_map: Dict mapping product_id to current stock
            
        Returns:
            Dict mapping product_id to prediction
        """
        results = {}
        for pid in product_ids:
            current_stock = current_stock_map.get(pid, 0) if current_stock_map else 0
            results[pid] = self.get_restock_prediction(pid, warehouse_id, current_stock)
        return results
    
    def force_retrain(self) -> None:
        """Force immediate model retraining."""
        event = ModelUpdateEvent(event_type="force_retrain")
        self._event_queue.put(event)
        logger.info("Forced retrain queued")
    
    def get_stats(self) -> Dict[str, Any]:
        """Get current statistics (thread-safe)."""
        with self._stats_lock:
            stats = self._stats.copy()
        
        stats.update({
            "queue_size": self._event_queue.qsize(),
            "cache_size": len(self._prediction_cache),
            "model_ready": self._model_ready,
            "last_update": self._last_update_time.isoformat() if self._last_update_time else None
        })
        return stats
    
    def register_update_callback(self, callback: Callable[[Dict[str, Any]], None]) -> None:
        """Register a callback to be called when model is updated."""
        self._update_callbacks.append(callback)


class BatchedEventProcessor:
    """
    Alternative batching mechanism that accumulates events and flushes periodically.
    
    This is a simpler approach that batches by time window rather than
    using a background thread for model updates.
    """
    
    def __init__(
        self,
        db_path: str = "orders_history.db",
        flush_interval_seconds: int = 30,
        max_batch_size: int = 50
    ):
        self.db_path = db_path
        self.flush_interval = flush_interval_seconds
        self.max_batch_size = max_batch_size
        
        self._batch: List[Dict[str, Any]] = []
        self._batch_lock = threading.Lock()
        self._last_flush = time.time()
        self._repository: Optional[OrderRepository] = None
        
        # Background flush timer
        self._timer: Optional[threading.Timer] = None
        self._shutdown = False
    
    def start(self) -> None:
        """Start the periodic flush timer."""
        self._repository = OrderRepository(self.db_path)
        self._schedule_next_flush()
        logger.info("BatchedEventProcessor started (interval=%ds)", self.flush_interval)
    
    def stop(self) -> None:
        """Stop the processor and flush remaining events."""
        self._shutdown = True
        if self._timer:
            self._timer.cancel()
        self._flush_batch()
        logger.info("BatchedEventProcessor stopped")
    
    def _schedule_next_flush(self) -> None:
        """Schedule the next flush timer."""
        if not self._shutdown:
            self._timer = threading.Timer(self.flush_interval, self._timed_flush)
            self._timer.daemon = True
            self._timer.start()
    
    def _timed_flush(self) -> None:
        """Timer callback for periodic flush."""
        self._flush_batch()
        self._schedule_next_flush()
    
    def add_event(self, order_data: Dict[str, Any]) -> None:
        """
        Add an event to the batch (non-blocking).
        
        Args:
            order_data: Order data to batch
        """
        with self._batch_lock:
            self._batch.append(order_data)
            should_flush = len(self._batch) >= self.max_batch_size
        
        if should_flush:
            self._flush_batch()
    
    def _flush_batch(self) -> None:
        """Flush accumulated batch to database."""
        with self._batch_lock:
            batch_to_flush = self._batch.copy()
            self._batch = []
        
        if not batch_to_flush:
            return
        
        logger.info("Flushing batch of %d orders", len(batch_to_flush))
        
        try:
            for order_data in batch_to_flush:
                if self._repository:
                    self._repository.save_order_items(order_data)
            
            self._last_flush = time.time()
            logger.debug("Batch flush completed")
            
        except Exception as e:
            logger.error("Error flushing batch: %s", e)
            # Put back failed events
            with self._batch_lock:
                self._batch.extend(batch_to_flush)
    
    def get_pending_count(self) -> int:
        """Get number of pending events in batch."""
        with self._batch_lock:
            return len(self._batch)


# Convenience factory function
def create_ml_integration(
    db_path: str = "orders_history.db",
    mode: str = "background",  # "background" or "batch"
    **kwargs
) -> Any:
    """
    Factory function to create ML integration.
    
    Args:
        db_path: Path to SQLite database
        mode: "background" for BackgroundMLModelManager or "batch" for BatchedEventProcessor
        **kwargs: Additional configuration options
        
    Returns:
        Configured ML integration instance
    """
    if mode == "background":
        return BackgroundMLModelManager(
            db_path=db_path,
            update_interval_seconds=kwargs.get("update_interval_seconds", 60),
            batch_size=kwargs.get("batch_size", 100),
            min_events_for_update=kwargs.get("min_events_for_update", 10),
            prediction_cache_ttl=kwargs.get("prediction_cache_ttl", 300)
        )
    elif mode == "batch":
        return BatchedEventProcessor(
            db_path=db_path,
            flush_interval_seconds=kwargs.get("flush_interval_seconds", 30),
            max_batch_size=kwargs.get("max_batch_size", 50)
        )
    else:
        raise ValueError(f"Unknown mode: {mode}")
