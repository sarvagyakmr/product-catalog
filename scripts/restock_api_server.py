"""
Restock Prediction API Server

FastAPI-based web server that exposes REST endpoints for restock predictions.
Integrates with the BackgroundMLModelManager for real-time predictions.

Endpoints:
    GET /restock/{product_id} - Get restock prediction for a product
    GET /health - Health check
    GET /stats - ML model statistics
    POST /predict/batch - Batch predictions
    GET /report/{warehouse_id} - Full restock report

Usage:
    python restock_api_server.py
    
    # With custom settings
    python restock_api_server.py --port 8000 --ml-interval 60
"""

import argparse
import logging
import sys
from typing import Optional, Dict, Any, List
from datetime import datetime
from contextlib import asynccontextmanager

import uvicorn
from fastapi import FastAPI, HTTPException, BackgroundTasks, Query
from fastapi.responses import JSONResponse
from pydantic import BaseModel, Field

from ml_model_manager import BackgroundMLModelManager, create_ml_integration
from order_repository import OrderRepository
from restock_predictor import RestockAnalyzer

# Configure logging
logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s - %(name)s - %(levelname)s - %(message)s",
    handlers=[logging.StreamHandler(sys.stdout)]
)
logger = logging.getLogger(__name__)

# Global state
ml_manager: Optional[BackgroundMLModelManager] = None
repository: Optional[OrderRepository] = None

# Default configuration (set before lifespan runs)
DEFAULT_DB_PATH = "orders_history.db"
DEFAULT_ML_INTERVAL = 60
DEFAULT_PORT = 8000


# ============ Pydantic Models ============

class RestockResponse(BaseModel):
    """Response model for restock prediction endpoint."""
    productId: int = Field(..., description="Product identifier")
    currentVelocity: float = Field(..., description="Average daily demand (units/day)")
    recommendedRestockQuantity: int = Field(..., description="Recommended units to restock")
    confidence: float = Field(..., description="Prediction confidence score (0-1)")
    daysOfStock: int = Field(..., description="Days of stock remaining")
    method: str = Field(..., description="Prediction method used")
    warehouseId: int = Field(..., description="Warehouse identifier")
    timestamp: str = Field(..., description="Prediction timestamp")
    
    model_config = {
        "json_schema_extra": {
            "example": {
                "productId": 42,
                "currentVelocity": 15.5,
                "recommendedRestockQuantity": 124,
                "confidence": 0.89,
                "daysOfStock": 3,
                "method": "ema_trend_weighted",
                "warehouseId": 1,
                "timestamp": "2024-01-15T10:30:00"
            }
        }
    }


class BatchPredictionRequest(BaseModel):
    """Request model for batch predictions."""
    productIds: List[int] = Field(..., description="List of product IDs")
    warehouseId: int = Field(default=1, description="Warehouse ID")
    currentStockMap: Optional[Dict[int, int]] = Field(
        default=None, 
        description="Map of product_id to current stock level"
    )


class BatchPredictionResponse(BaseModel):
    """Response model for batch predictions."""
    predictions: List[RestockResponse]
    totalRequested: int
    totalSuccessful: int
    timestamp: str


class HealthResponse(BaseModel):
    """Health check response."""
    status: str
    modelReady: bool
    timestamp: str
    version: str = "1.0.0"


class StatsResponse(BaseModel):
    """ML model statistics response."""
    eventsReceived: int
    eventsProcessed: int
    modelUpdates: int
    predictionsServed: int
    cacheHits: int
    cacheMisses: int
    queueSize: int
    cacheSize: int
    modelReady: bool
    lastUpdate: Optional[str]
    cacheHitRate: float


class RestockReportResponse(BaseModel):
    """Full restock report response."""
    warehouseId: int
    generatedAt: str
    summary: Dict[str, Any]
    recommendations: List[Dict[str, Any]]
    lowStockAlerts: List[Dict[str, Any]]


# ============ FastAPI Application ============

@asynccontextmanager
async def lifespan(app: FastAPI):
    """Application lifespan manager - startup and shutdown."""
    # Startup
    logger.info("Starting Restock Prediction API Server...")
    
    global ml_manager, repository
    
    # Get configuration from app state or use defaults
    db_path = getattr(app.state, 'db_path', DEFAULT_DB_PATH)
    ml_interval = getattr(app.state, 'ml_interval', DEFAULT_ML_INTERVAL)
    port = getattr(app.state, 'port', DEFAULT_PORT)
    
    # Initialize repository
    try:
        repository = OrderRepository(db_path)
        logger.info("Repository initialized: %s", db_path)
    except Exception as e:
        logger.error("Failed to initialize repository: %s", e)
        raise
    
    # Initialize ML manager
    try:
        ml_manager = BackgroundMLModelManager(
            db_path=db_path,
            update_interval_seconds=ml_interval,
            batch_size=100,
            prediction_cache_ttl=300
        )
        ml_manager.start()
        logger.info("ML Model Manager started (update_interval=%ds)", ml_interval)
    except Exception as e:
        logger.error("Failed to start ML manager: %s", e)
        raise
    
    logger.info("API Server ready - Listening on port %d", port)
    
    yield
    
    # Shutdown
    logger.info("Shutting down API Server...")
    if ml_manager:
        ml_manager.stop()
    logger.info("API Server stopped")


# Create FastAPI app
app = FastAPI(
    title="Restock Prediction API",
    description="ML-based restock prediction service with real-time velocity calculations",
    version="1.0.0",
    lifespan=lifespan
)


# ============ API Endpoints ============

@app.get("/", response_model=Dict[str, str])
async def root():
    """Root endpoint with API information."""
    return {
        "service": "Restock Prediction API",
        "version": "1.0.0",
        "docs": "/docs",
        "health": "/health"
    }


@app.get("/health", response_model=HealthResponse)
async def health_check():
    """
    Health check endpoint.
    
    Returns:
        Health status of the API and ML model
    """
    if not ml_manager:
        raise HTTPException(status_code=503, detail="ML Manager not initialized")
    
    stats = ml_manager.get_stats()
    
    return HealthResponse(
        status="healthy" if stats["model_ready"] else "degraded",
        modelReady=stats["model_ready"],
        timestamp=datetime.now().isoformat()
    )


@app.get(
    "/restock/{product_id}",
    response_model=RestockResponse,
    summary="Get restock prediction for a product",
    description="Returns velocity and recommended restock quantity for a specific product"
)
async def get_restock_prediction(
    product_id: int,
    warehouse_id: int = Query(default=1, description="Warehouse ID"),
    current_stock: int = Query(default=0, description="Current stock level")
):
    """
    Get restock prediction for a specific product.
    
    Args:
        product_id: Product identifier
        warehouse_id: Warehouse ID (default: 1)
        current_stock: Current inventory level (default: 0)
        
    Returns:
        Restock prediction with velocity and recommended quantity
        
    Raises:
        404: If product has no historical data
        503: If ML model is not ready
    """
    if not ml_manager or not ml_manager._model_ready:
        raise HTTPException(
            status_code=503,
            detail="ML model is not ready. Please try again later."
        )
    
    # Get prediction from ML manager
    prediction = ml_manager.get_restock_prediction(
        product_id=product_id,
        warehouse_id=warehouse_id,
        current_stock=current_stock,
        use_cache=True
    )
    
    if not prediction:
        raise HTTPException(
            status_code=404,
            detail=f"No prediction available for product {product_id}. "
                   "Insufficient historical data."
        )
    
    # Handle infinity values for daysOfStock (when no demand but stock exists)
    days_of_stock = prediction.days_of_stock
    if days_of_stock == float('inf'):
        days_of_stock = 999  # Use sentinel value for "infinite"
    
    return RestockResponse(
        productId=prediction.product_id,
        currentVelocity=round(prediction.avg_daily_demand, 2),
        recommendedRestockQuantity=prediction.recommended_quantity,
        confidence=round(prediction.confidence_score, 3),
        daysOfStock=days_of_stock,
        method=prediction.prediction_method,
        warehouseId=prediction.warehouse_id,
        timestamp=datetime.now().isoformat()
    )


@app.post(
    "/predict/batch",
    response_model=BatchPredictionResponse,
    summary="Get batch predictions",
    description="Get restock predictions for multiple products in a single request"
)
async def batch_predictions(request: BatchPredictionRequest):
    """
    Get predictions for multiple products.
    
    Args:
        request: BatchPredictionRequest with product IDs and optional stock levels
        
    Returns:
        List of predictions for all requested products
    """
    if not ml_manager or not ml_manager._model_ready:
        raise HTTPException(
            status_code=503,
            detail="ML model is not ready"
        )
    
    # Get batch predictions
    results = ml_manager.get_batch_predictions(
        product_ids=request.productIds,
        warehouse_id=request.warehouseId,
        current_stock_map=request.currentStockMap or {}
    )
    
    # Convert to response models
    predictions = []
    successful = 0
    
    for product_id in request.productIds:
        pred = results.get(product_id)
        if pred:
            # Handle infinity values for daysOfStock
            days_of_stock = pred.days_of_stock
            if days_of_stock == float('inf'):
                days_of_stock = 999
            
            predictions.append(RestockResponse(
                productId=pred.product_id,
                currentVelocity=round(pred.avg_daily_demand, 2),
                recommendedRestockQuantity=pred.recommended_quantity,
                confidence=round(pred.confidence_score, 3),
                daysOfStock=days_of_stock,
                method=pred.prediction_method,
                warehouseId=pred.warehouse_id,
                timestamp=datetime.now().isoformat()
            ))
            successful += 1
        else:
            # Include failed predictions with zeros
            predictions.append(RestockResponse(
                productId=product_id,
                currentVelocity=0.0,
                recommendedRestockQuantity=0,
                confidence=0.0,
                daysOfStock=0,
                method="no_data",
                warehouseId=request.warehouseId,
                timestamp=datetime.now().isoformat()
            ))
    
    return BatchPredictionResponse(
        predictions=predictions,
        totalRequested=len(request.productIds),
        totalSuccessful=successful,
        timestamp=datetime.now().isoformat()
    )


@app.get(
    "/stats",
    response_model=StatsResponse,
    summary="Get ML model statistics",
    description="Returns operational statistics about the ML model"
)
async def get_stats():
    """
    Get ML model manager statistics.
    
    Returns:
        Statistics about events processed, cache performance, etc.
    """
    if not ml_manager:
        raise HTTPException(status_code=503, detail="ML Manager not initialized")
    
    stats = ml_manager.get_stats()
    total_requests = stats["cache_hits"] + stats["cache_misses"]
    hit_rate = (stats["cache_hits"] / total_requests * 100) if total_requests > 0 else 0
    
    return StatsResponse(
        eventsReceived=stats["events_received"],
        eventsProcessed=stats["events_processed"],
        modelUpdates=stats["model_updates"],
        predictionsServed=stats["predictions_served"],
        cacheHits=stats["cache_hits"],
        cacheMisses=stats["cache_misses"],
        queueSize=stats["queue_size"],
        cacheSize=stats["cache_size"],
        modelReady=stats["model_ready"],
        lastUpdate=stats["last_update"],
        cacheHitRate=round(hit_rate, 2)
    )


@app.get(
    "/report/{warehouse_id}",
    response_model=RestockReportResponse,
    summary="Get full restock report",
    description="Generate a comprehensive restock report for a warehouse"
)
async def get_restock_report(
    warehouse_id: int,
    min_quantity: int = Query(default=1, description="Minimum recommended quantity to include")
):
    """
    Generate a comprehensive restock report for a warehouse.
    
    Args:
        warehouse_id: Warehouse ID
        min_quantity: Minimum recommended quantity filter
        
    Returns:
        Full report with recommendations and low stock alerts
    """
    if not repository:
        raise HTTPException(status_code=503, detail="Repository not initialized")
    
    try:
        analyzer = RestockAnalyzer(repository.db_path)
        report = analyzer.generate_restock_report(warehouse_id, min_quantity)
        
        return RestockReportResponse(
            warehouseId=warehouse_id,
            generatedAt=report["generated_at"],
            summary=report["summary"],
            recommendations=report["recommendations"],
            lowStockAlerts=report["low_stock_alerts"]
        )
    except Exception as e:
        logger.error("Error generating report: %s", e)
        raise HTTPException(status_code=500, detail=str(e))


@app.post("/retrain")
async def force_retrain(background_tasks: BackgroundTasks):
    """
    Force immediate model retraining.
    
    This triggers a background model update. Use sparingly as it
    may impact performance.
    
    Returns:
        Confirmation that retrain was queued
    """
    if not ml_manager:
        raise HTTPException(status_code=503, detail="ML Manager not initialized")
    
    ml_manager.force_retrain()
    
    return {
        "status": "queued",
        "message": "Model retrain queued and will execute shortly",
        "timestamp": datetime.now().isoformat()
    }


# ============ Main Entry Point ============

def main():
    """Main entry point for the API server."""
    parser = argparse.ArgumentParser(
        description="Restock Prediction API Server"
    )
    parser.add_argument(
        "--host",
        default="0.0.0.0",
        help="Host to bind to (default: 0.0.0.0)"
    )
    parser.add_argument(
        "--port",
        type=int,
        default=8000,
        help="Port to listen on (default: 8000)"
    )
    parser.add_argument(
        "--db-path",
        default="orders_history.db",
        help="Path to SQLite database (default: orders_history.db)"
    )
    parser.add_argument(
        "--ml-interval",
        type=int,
        default=60,
        help="ML model update interval in seconds (default: 60)"
    )
    parser.add_argument(
        "--reload",
        action="store_true",
        help="Enable auto-reload for development"
    )
    
    args = parser.parse_args()
    
    # Store config in app state
    app.state.port = args.port
    app.state.db_path = args.db_path
    app.state.ml_interval = args.ml_interval
    
    logger.info("Starting Restock Prediction API Server")
    logger.info("Database: %s", args.db_path)
    logger.info("ML Update Interval: %ds", args.ml_interval)
    logger.info("API Documentation: http://%s:%d/docs", args.host, args.port)
    
    # Run server
    uvicorn.run(
        "restock_api_server:app",
        host=args.host,
        port=args.port,
        reload=args.reload,
        log_level="info"
    )


if __name__ == "__main__":
    main()
