# Restock Prediction API Server

FastAPI-based REST API for ML-powered restock predictions.

## Quick Start

```bash
# Install dependencies
pip install -r requirements.txt

# Start the API server
python restock_api_server.py

# Or with custom settings
python restock_api_server.py --port 8000 --ml-interval 60 --db-path orders_history.db
```

## API Documentation

Once running, access interactive docs at: `http://localhost:8000/docs`

## Endpoints

### GET /restock/{product_id}

Get restock prediction for a specific product.

**Parameters:**
- `product_id` (path): Product identifier
- `warehouse_id` (query): Warehouse ID (default: 1)
- `current_stock` (query): Current inventory level (default: 0)

**Response:**
```json
{
  "productId": 42,
  "currentVelocity": 15.5,
  "recommendedRestockQuantity": 124,
  "confidence": 0.89,
  "daysOfStock": 3,
  "method": "ema_trend_weighted",
  "warehouseId": 1,
  "timestamp": "2024-01-15T10:30:00"
}
```

**Example:**
```bash
curl "http://localhost:8000/restock/42?warehouse_id=1&current_stock=50"
```

### POST /predict/batch

Get predictions for multiple products.

**Request:**
```json
{
  "productIds": [1, 2, 3, 4, 5],
  "warehouseId": 1,
  "currentStockMap": {
    "1": 50,
    "2": 30,
    "3": 0
  }
}
```

**Response:**
```json
{
  "predictions": [
    {
      "productId": 1,
      "currentVelocity": 12.4,
      "recommendedRestockQuantity": 87,
      ...
    }
  ],
  "totalRequested": 5,
  "totalSuccessful": 5,
  "timestamp": "2024-01-15T10:30:00"
}
```

### GET /health

Health check endpoint.

**Response:**
```json
{
  "status": "healthy",
  "modelReady": true,
  "timestamp": "2024-01-15T10:30:00",
  "version": "1.0.0"
}
```

### GET /stats

ML model statistics.

**Response:**
```json
{
  "eventsReceived": 1523,
  "eventsProcessed": 1523,
  "modelUpdates": 25,
  "predictionsServed": 89,
  "cacheHits": 67,
  "cacheMisses": 22,
  "queueSize": 0,
  "cacheSize": 15,
  "modelReady": true,
  "lastUpdate": "2024-01-15T10:30:00",
  "cacheHitRate": 75.28
}
```

### GET /report/{warehouse_id}

Full restock report for a warehouse.

**Parameters:**
- `warehouse_id` (path): Warehouse ID
- `min_quantity` (query): Minimum recommended quantity filter (default: 1)

**Response:**
```json
{
  "warehouseId": 1,
  "generatedAt": "2024-01-15T10:30:00",
  "summary": {
    "totalProductsAnalyzed": 15,
    "productsNeedingRestock": 12,
    "totalUnitsToRestock": 1847
  },
  "recommendations": [...],
  "lowStockAlerts": [...]
}
```

### POST /retrain

Force immediate model retraining.

**Response:**
```json
{
  "status": "queued",
  "message": "Model retrain queued and will execute shortly",
  "timestamp": "2024-01-15T10:30:00"
}
```

## Architecture

```
┌─────────────────┐
│   FastAPI       │
│   Server        │
│   (Port 8000)   │
└────────┬────────┘
         │
         ▼
┌─────────────────────────┐
│  BackgroundMLModelManager│
│  - In-memory model       │
│  - Prediction cache      │
│  - Periodic updates      │
└────────┬────────────────┘
         │
         ▼
┌─────────────────────────┐
│   SQLite Database       │
│   (orders_history.db)   │
└─────────────────────────┘
```

## Integration with Redis Subscriber

The API server can run standalone or alongside the Redis subscriber:

```bash
# Terminal 1: Start Redis subscriber to collect events
python redis_ml_subscriber.py

# Terminal 2: Start API server to serve predictions
python restock_api_server.py --port 8000
```

## Configuration

| Option | Default | Description |
|--------|---------|-------------|
| `--host` | 0.0.0.0 | Bind address |
| `--port` | 8000 | Port to listen on |
| `--db-path` | orders_history.db | SQLite database path |
| `--ml-interval` | 60 | ML update interval (seconds) |
| `--reload` | false | Auto-reload for development |

## Performance

| Endpoint | Latency (cache hit) | Latency (cache miss) |
|----------|--------------------|---------------------|
| GET /restock/{id} | < 1ms | 10-50ms |
| POST /predict/batch | < 5ms | 50-200ms |
| GET /health | < 1ms | < 1ms |
| GET /stats | < 1ms | < 1ms |

## Testing

```bash
# Start server
python restock_api_server.py &

# Test health endpoint
curl http://localhost:8000/health

# Get prediction
curl "http://localhost:8000/restock/1?warehouse_id=1&current_stock=50"

# Batch predictions
curl -X POST http://localhost:8000/predict/batch \
  -H "Content-Type: application/json" \
  -d '{"productIds": [1, 2, 3], "warehouseId": 1}'

# Get stats
curl http://localhost:8000/stats

# Get report
curl http://localhost:8000/report/1
```
