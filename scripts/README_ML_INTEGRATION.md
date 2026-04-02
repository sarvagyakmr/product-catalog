# ML-Integrated Redis Subscriber

This document describes the integration between the Redis event subscriber and the ML-based restock prediction system with non-blocking background updates.

## Architecture Overview

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                        ML-INTEGRATED SUBSCRIBER                              │
└─────────────────────────────────────────────────────────────────────────────┘

┌─────────────────┐     ┌──────────────────┐     ┌─────────────────────────┐
│   Redis Pub/Sub │────►│  Event Processor │────►│   SQLite Storage        │
│   (Spring Boot) │     │  (Non-blocking)  │     │   (orders_history.db)   │
└─────────────────┘     └────────┬─────────┘     └─────────────────────────┘
                                 │
                                 ▼
                    ┌────────────────────────┐
                    │   Event Queue          │
                    │   (Thread-safe)        │
                    └────────┬───────────────┘
                             │
                             ▼
                    ┌────────────────────────┐
                    │  Background Worker     │
                    │  (ML Model Updater)    │
                    │                        │
                    │  ┌──────────────────┐  │
                    │  │  Periodic Update │  │
                    │  │  (every N sec)   │  │
                    │  └──────────────────┘  │
                    │  ┌──────────────────┐  │
                    │  │  Batch Process   │  │
                    │  │  (if queue > N)  │  │
                    │  └──────────────────┘  │
                    │  ┌──────────────────┐  │
                    │  │  Cache Pre-compute│  │
                    │  │  (popular items) │  │
                    │  └──────────────────┘  │
                    └────────┬───────────────┘
                             │
                             ▼
                    ┌────────────────────────┐
                    │  Prediction Cache      │
                    │  (Thread-safe)         │
                    └────────┬───────────────┘
                             │
        ┌────────────────────┼────────────────────┐
        │                    │                    │
        ▼                    ▼                    ▼
┌──────────────┐    ┌──────────────┐    ┌──────────────┐
│   Command    │    │   Real-time  │    │   Periodic   │
│   Interface  │    │   Predictions│    │   Reports    │
│   (interactive│    │   (fast API) │    │   (stats)    │
└──────────────┘    └──────────────┘    └──────────────┘
```

## Key Components

### 1. `ml_model_manager.py` - Core ML Management

#### `BackgroundMLModelManager`
Thread-safe ML model manager that runs updates in a background thread.

**Features:**
- **Event Queue**: Buffers incoming order events without blocking
- **Background Worker**: Processes events and updates model periodically
- **Prediction Cache**: Thread-safe cache for fast lookups
- **Batch Processing**: Processes accumulated events in batches
- **Automatic Retraining**: Triggers updates based on thresholds

**Configuration:**
```python
manager = BackgroundMLModelManager(
    db_path="orders_history.db",
    update_interval_seconds=60,    # How often to update model
    batch_size=100,                 # Events per batch
    min_events_for_update=10,       # Min events to trigger update
    prediction_cache_ttl=300        # Cache TTL in seconds
)
```

**Public API:**
- `on_new_order_event(order_data)` - Non-blocking event ingestion
- `get_restock_prediction(product_id, ...)` - Fast cached predictions
- `get_batch_predictions(product_ids, ...)` - Batch predictions
- `force_retrain()` - Trigger immediate retrain
- `get_stats()` - Get operational statistics

#### `BatchedEventProcessor` (Alternative)
Simpler batching mechanism using timers instead of background thread.

### 2. `redis_ml_subscriber.py` - Integrated Subscriber

Combines Redis subscription with ML processing.

**Features:**
- Listens to Redis channels (`allocated-orders`, `cancelled-orders`)
- Persists events to SQLite
- Feeds events to ML manager (non-blocking)
- Interactive command interface
- Periodic statistics reporting

**Usage:**
```bash
python redis_ml_subscriber.py --ml-interval 60
```

**Interactive Commands:**
- `predict <product_id> [warehouse] [stock]` - Get prediction
- `stats` - Show ML manager statistics
- `report [warehouse_id]` - Generate restock report
- `cache` - Show cache statistics
- `retrain` - Force model retrain
- `batch <id1> <id2> ...` - Batch predictions
- `help` - Show help
- `quit` - Stop subscriber

## Non-Blocking Design

### Event Flow

1. **Redis Message Received** → Main thread decodes JSON (< 1ms)
2. **Persist to SQLite** → Local write, non-blocking (< 10ms)
3. **Queue Event** → `queue.put_nowait()` returns immediately (< 1ms)
4. **Background Worker** → Processes queue independently
5. **Model Update** → Runs periodically (60s default)

### Thread Safety

| Component | Thread Safety Mechanism |
|-----------|------------------------|
| Event Queue | `queue.Queue` (thread-safe) |
| Prediction Cache | `threading.RLock` |
| Statistics | `threading.Lock` |
| Database | SQLite + context managers |

## Background Update Process

```
┌─────────────────────────────────────────────────────────────┐
│                    BACKGROUND WORKER                         │
└─────────────────────────────────────────────────────────────┘

Every 60 seconds (configurable):
┌─────────────┐
│   Start     │
└──────┬──────┘
       ▼
┌─────────────┐     ┌─────────────────────────────┐
│ Check Queue │────►│ Process Events (if any)     │
│             │     │ - Update cache invalidations│
└─────────────┘     └─────────────────────────────┘
       │
       ▼
┌─────────────┐     ┌─────────────────────────────┐
│ Clear Expired│────►│ Remove stale cache entries │
│   Cache     │     └─────────────────────────────┘
└─────────────┘
       │
       ▼
┌─────────────┐     ┌─────────────────────────────┐
│ Pre-compute │────►│ Calculate predictions for   │
│ Popular Items│     │ top N products              │
└─────────────┘     └─────────────────────────────┘
       │
       ▼
┌─────────────┐
│    End      │
└─────────────┘
```

## ML Algorithm Updates

### Statistical Methods

| Method | Purpose | Update Frequency |
|--------|---------|------------------|
| Simple Moving Average (SMA) | Baseline demand | Every background update |
| Exponential Moving Average (EMA) | Trend detection | Every background update |
| Linear Regression Slope | Trend direction | Every background update |
| Safety Stock Calculation | Buffer inventory | On prediction request |
| Confidence Score | Prediction quality | On prediction request |

### Cache Invalidation

When a new order arrives:
1. Parse order items
2. For each product in order:
   - Remove product from prediction cache
   - Next prediction will be recalculated fresh

## Usage Examples

### Basic Integration

```python
from ml_model_manager import BackgroundMLModelManager

# Create and start manager
manager = BackgroundMLModelManager(
    db_path="orders_history.db",
    update_interval_seconds=60
)
manager.start()

# In Redis subscriber callback
def on_order_received(order_data):
    # Non-blocking - returns immediately
    manager.on_new_order_event(order_data)
    
    # Can also get prediction (uses cache)
    prediction = manager.get_restock_prediction(
        product_id=order_data['items'][0]['productId']
    )
```

### Running the Integrated Subscriber

```bash
# Start with default settings (60s ML update interval)
python redis_ml_subscriber.py

# Custom settings
python redis_ml_subscriber.py \
    --host localhost \
    --port 6379 \
    --db-path orders_history.db \
    --ml-interval 30

# Interactive commands (while running)
[ML-Subscriber] predict 42 1 50
[ML-Subscriber] stats
[ML-Subscriber] report 1
[ML-Subscriber] batch 1 2 3 4 5
```

### Programmatic API

```python
from redis_ml_subscriber import RedisMLSubscriber

subscriber = RedisMLSubscriber(
    redis_host="localhost",
    redis_port=6379,
    db_path="orders_history.db",
    ml_update_interval=60
)

subscriber.start()  # Blocks until interrupted
```

## Performance Characteristics

| Operation | Latency | Thread |
|-----------|---------|--------|
| Redis Event Receive | ~1ms | Main |
| JSON Decode | <1ms | Main |
| SQLite Insert | 5-10ms | Main |
| Queue Event | <1ms | Main |
| **Total Event Handling** | **<15ms** | **Main** |
| Background Batch Process | 10-100ms | Background |
| Cache Pre-computation | 100-500ms | Background |
| Prediction (cache hit) | <1ms | Main |
| Prediction (cache miss) | 10-50ms | Main |

## Monitoring

### Statistics Available

```python
stats = manager.get_stats()

{
    "events_received": 1523,
    "events_processed": 1523,
    "model_updates": 25,
    "predictions_served": 89,
    "cache_hits": 67,
    "cache_misses": 22,
    "queue_size": 0,
    "cache_size": 15,
    "model_ready": True,
    "last_update": "2024-01-15T10:30:00"
}
```

### Periodic Logging

Every 5 minutes, stats are automatically logged:
```
[Stats] Events: 1523 processed, 0 queued | Cache: 15 entries, 67 hits | Updates: 25
```

## Configuration Tuning

### High-Volume Scenarios

```python
# For high event volume (>1000 events/minute)
manager = BackgroundMLModelManager(
    update_interval_seconds=30,   # Update more frequently
    batch_size=500,               # Larger batches
    min_events_for_update=50,     # Higher threshold
    prediction_cache_ttl=60       # Shorter cache TTL
)
```

### Low-Latency Scenarios

```python
# For faster predictions
manager = BackgroundMLModelManager(
    update_interval_seconds=300,  # Update less frequently
    batch_size=1000,              # Larger batches
    prediction_cache_ttl=600      # Longer cache TTL
)
```

## Files Overview

| File | Purpose |
|------|---------|
| `ml_model_manager.py` | Background ML model management |
| `redis_ml_subscriber.py` | Integrated Redis + ML subscriber |
| `order_repository.py` | SQLite storage layer |
| `restock_predictor.py` | ML prediction algorithms |

## Testing

```bash
# 1. Start the integrated subscriber
python redis_ml_subscriber.py &

# 2. Generate mock orders (in another terminal)
python generate_mock_products.py

# 3. In subscriber console, check predictions
[ML-Subscriber] predict 1 1 50
[ML-Subscriber] stats

# 4. Generate more orders and watch updates
[ML-Subscriber] report 1
```
