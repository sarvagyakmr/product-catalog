"""
Restock Predictor Module

Provides statistical moving average-based predictions for inventory restocking.
Uses historical order data to calculate optimal restock quantities.
"""

import logging
from typing import Dict, List, Any, Optional, Tuple
from dataclasses import dataclass
from datetime import datetime, timedelta
from collections import deque
import statistics

from order_repository import OrderRepository

logger = logging.getLogger(__name__)


@dataclass
class RestockRecommendation:
    """Represents a restock recommendation for a product."""
    product_id: int
    warehouse_id: int
    current_stock: int  # Assumed or provided
    recommended_quantity: int
    confidence_score: float  # 0.0 to 1.0
    avg_daily_demand: float
    demand_volatility: float  # Standard deviation
    days_of_stock: int  # How many days current stock will last
    prediction_method: str
    
    def to_dict(self) -> Dict[str, Any]:
        return {
            "product_id": self.product_id,
            "warehouse_id": self.warehouse_id,
            "current_stock": self.current_stock,
            "recommended_quantity": self.recommended_quantity,
            "confidence_score": round(self.confidence_score, 3),
            "avg_daily_demand": round(self.avg_daily_demand, 2),
            "demand_volatility": round(self.demand_volatility, 2),
            "days_of_stock": self.days_of_stock,
            "prediction_method": self.prediction_method
        }


class MovingAverageRestockPredictor:
    """
    Statistical moving average-based restock predictor.
    
    Calculates restock quantities based on:
    - Simple Moving Average (SMA) of daily demand
    - Exponential Moving Average (EMA) for trend detection
    - Demand volatility (standard deviation)
    - Safety stock based on service level
    """
    
    def __init__(
        self,
        repository: OrderRepository,
        sma_window: int = 7,  # 7-day simple moving average
        ema_alpha: float = 0.3,  # EMA smoothing factor
        safety_factor: float = 1.65,  # 95% service level (z-score)
        lead_time_days: int = 3,  # Days to receive new stock
        review_period_days: int = 7  # How often to review stock
    ):
        self.repository = repository
        self.sma_window = sma_window
        self.ema_alpha = ema_alpha
        self.safety_factor = safety_factor
        self.lead_time_days = lead_time_days
        self.review_period_days = review_period_days
    
    def calculate_sma(self, values: List[float], window: int) -> float:
        """
        Calculate Simple Moving Average.
        
        Args:
            values: List of historical values
            window: Number of periods to average
            
        Returns:
            Simple moving average
        """
        if not values:
            return 0.0
        
        # Use most recent 'window' values
        recent_values = values[-window:] if len(values) >= window else values
        return statistics.mean(recent_values)
    
    def calculate_ema(self, values: List[float]) -> float:
        """
        Calculate Exponential Moving Average.
        
        EMA gives more weight to recent observations.
        Formula: EMA_t = alpha * value_t + (1 - alpha) * EMA_{t-1}
        
        Args:
            values: List of historical values (oldest first)
            
        Returns:
            Exponential moving average
        """
        if not values:
            return 0.0
        
        ema = values[0]  # Initialize with first value
        for value in values[1:]:
            ema = self.ema_alpha * value + (1 - self.ema_alpha) * ema
        return ema
    
    def calculate_volatility(self, values: List[float]) -> float:
        """
        Calculate demand volatility (standard deviation).
        
        Args:
            values: List of historical demand values
            
        Returns:
            Standard deviation of demand
        """
        if len(values) < 2:
            return 0.0
        return statistics.stdev(values)
    
    def calculate_trend(self, values: List[float]) -> float:
        """
        Calculate trend direction using linear regression slope.
        
        Args:
            values: List of historical values
            
        Returns:
            Slope indicating trend (positive = increasing demand)
        """
        if len(values) < 2:
            return 0.0
        
        n = len(values)
        x_vals = list(range(n))
        y_vals = values
        
        # Simple linear regression slope
        x_mean = statistics.mean(x_vals)
        y_mean = statistics.mean(y_vals)
        
        numerator = sum((x - x_mean) * (y - y_mean) for x, y in zip(x_vals, y_vals))
        denominator = sum((x - x_mean) ** 2 for x in x_vals)
        
        if denominator == 0:
            return 0.0
        
        return numerator / denominator
    
    def predict_restock(
        self,
        product_id: int,
        warehouse_id: int,
        current_stock: int = 0,
        history_days: int = 30
    ) -> RestockRecommendation:
        """
        Calculate restock recommendation for a product.
        
        Algorithm:
        1. Get historical daily demand
        2. Calculate SMA and EMA
        3. Compute demand volatility
        4. Determine safety stock (volatility * safety factor)
        5. Calculate demand during lead time + review period
        6. Recommend quantity = (demand forecast + safety stock) - current stock
        
        Args:
            product_id: Product ID to analyze
            warehouse_id: Warehouse ID
            current_stock: Current inventory level
            history_days: Days of history to analyze
            
        Returns:
            RestockRecommendation with calculated values
        """
        # Get daily stats from repository
        daily_stats = self.repository.get_daily_stats(
            product_id, warehouse_id, days=history_days
        )
        
        if not daily_stats:
            logger.warning(
                "No historical data for product %s in warehouse %s", 
                product_id, warehouse_id
            )
            return RestockRecommendation(
                product_id=product_id,
                warehouse_id=warehouse_id,
                current_stock=current_stock,
                recommended_quantity=0,
                confidence_score=0.0,
                avg_daily_demand=0.0,
                demand_volatility=0.0,
                days_of_stock=float('inf') if current_stock > 0 else 0,
                prediction_method="no_data"
            )
        
        # Extract daily quantities
        daily_quantities = [stat["total_quantity"] for stat in daily_stats]
        
        # Calculate moving averages
        sma = self.calculate_sma(daily_quantities, self.sma_window)
        ema = self.calculate_ema(daily_quantities)
        
        # Calculate volatility
        volatility = self.calculate_volatility(daily_quantities)
        
        # Calculate trend
        trend = self.calculate_trend(daily_quantities)
        
        # Combine SMA and EMA for forecast (weighted average)
        # EMA gets more weight if trend is strong
        trend_weight = min(abs(trend) / max(sma, 1), 0.5)  # Max 50% weight to trend
        forecast_demand = (1 - trend_weight) * sma + trend_weight * ema
        
        # Ensure forecast is non-negative
        forecast_demand = max(forecast_demand, 0)
        
        # Calculate safety stock
        # Safety stock = Z * sigma * sqrt(lead_time)
        # Where Z = service level factor, sigma = demand std dev
        safety_stock = self.safety_factor * volatility * (self.lead_time_days ** 0.5)
        
        # Calculate demand during protection period (lead time + review period)
        protection_period = self.lead_time_days + self.review_period_days
        demand_during_protection = forecast_demand * protection_period
        
        # Calculate optimal order quantity
        target_inventory = demand_during_protection + safety_stock
        recommended_quantity = max(0, target_inventory - current_stock)
        
        # Round to integer
        recommended_quantity = int(round(recommended_quantity))
        
        # Calculate confidence score based on data quality
        # More historical data and lower volatility = higher confidence
        data_points = len(daily_quantities)
        volatility_factor = 1 / (1 + volatility / max(forecast_demand, 1))
        data_factor = min(data_points / self.sma_window, 1.0)
        confidence_score = volatility_factor * data_factor
        
        # Calculate days of stock remaining
        days_of_stock = int(current_stock / forecast_demand) if forecast_demand > 0 else float('inf')
        
        # Determine prediction method used
        if trend_weight > 0.3:
            prediction_method = "ema_trend_weighted"
        else:
            prediction_method = "sma_baseline"
        
        recommendation = RestockRecommendation(
            product_id=product_id,
            warehouse_id=warehouse_id,
            current_stock=current_stock,
            recommended_quantity=recommended_quantity,
            confidence_score=confidence_score,
            avg_daily_demand=forecast_demand,
            demand_volatility=volatility,
            days_of_stock=days_of_stock,
            prediction_method=prediction_method
        )
        
        logger.info(
            "Restock prediction for product %s: qty=%s, confidence=%.2f, method=%s",
            product_id, recommended_quantity, confidence_score, prediction_method
        )
        
        return recommendation
    
    def predict_for_all_products(
        self,
        warehouse_id: int,
        current_stock_levels: Optional[Dict[int, int]] = None
    ) -> List[RestockRecommendation]:
        """
        Generate restock predictions for all products with historical data.
        
        Args:
            warehouse_id: Warehouse to analyze
            current_stock_levels: Dict mapping product_id to current stock.
                                 If None, assumes zero stock for all.
            
        Returns:
            List of RestockRecommendation objects
        """
        products = self.repository.get_all_products()
        recommendations = []
        
        for product_id in products:
            current_stock = current_stock_levels.get(product_id, 0) if current_stock_levels else 0
            
            try:
                recommendation = self.predict_restock(
                    product_id=product_id,
                    warehouse_id=warehouse_id,
                    current_stock=current_stock
                )
                recommendations.append(recommendation)
            except Exception as e:
                logger.error(
                    "Failed to predict for product %s: %s", 
                    product_id, e
                )
        
        # Sort by recommended quantity (descending) - prioritize high-need items
        recommendations.sort(key=lambda x: x.recommended_quantity, reverse=True)
        
        return recommendations
    
    def get_demand_forecast(
        self,
        product_id: int,
        warehouse_id: int,
        forecast_days: int = 14,
        history_days: int = 30
    ) -> List[Dict[str, Any]]:
        """
        Generate daily demand forecast for future dates.
        
        Args:
            product_id: Product to forecast
            warehouse_id: Warehouse ID
            forecast_days: Number of future days to forecast
            history_days: Days of history to use
            
        Returns:
            List of daily forecast dictionaries
        """
        daily_stats = self.repository.get_daily_stats(
            product_id, warehouse_id, days=history_days
        )
        
        if not daily_stats:
            return []
        
        daily_quantities = [stat["total_quantity"] for stat in daily_stats]
        sma = self.calculate_sma(daily_quantities, self.sma_window)
        trend = self.calculate_trend(daily_quantities)
        
        forecasts = []
        today = datetime.now().date()
        
        for day_offset in range(1, forecast_days + 1):
            forecast_date = today + timedelta(days=day_offset)
            # Project trend forward
            trend_adjustment = trend * day_offset
            forecast_value = max(0, sma + trend_adjustment)
            
            forecasts.append({
                "date": forecast_date.isoformat(),
                "forecasted_demand": round(forecast_value, 2),
                "trend_factor": round(trend_adjustment, 2)
            })
        
        return forecasts


class RestockAnalyzer:
    """
    High-level analyzer for restock operations.
    
    Combines repository access with ML predictions and provides
    convenient methods for common analysis tasks.
    """
    
    def __init__(self, db_path: str = "orders_history.db"):
        self.repository = OrderRepository(db_path)
        self.predictor = MovingAverageRestockPredictor(self.repository)
    
    def analyze_product(
        self,
        product_id: int,
        warehouse_id: int,
        current_stock: int = 0
    ) -> Dict[str, Any]:
        """
        Comprehensive analysis of a single product's restock needs.
        
        Args:
            product_id: Product to analyze
            warehouse_id: Warehouse ID
            current_stock: Current stock level
            
        Returns:
            Dictionary with recommendation and forecast
        """
        recommendation = self.predictor.predict_restock(
            product_id, warehouse_id, current_stock
        )
        
        forecast = self.predictor.get_demand_forecast(
            product_id, warehouse_id, forecast_days=14
        )
        
        history = self.repository.get_daily_stats(
            product_id, warehouse_id, days=30
        )
        
        return {
            "product_id": product_id,
            "warehouse_id": warehouse_id,
            "recommendation": recommendation.to_dict(),
            "demand_forecast_14d": forecast,
            "historical_summary": {
                "days_analyzed": len(history),
                "total_demand": sum(d["total_quantity"] for d in history),
                "avg_daily_demand": round(
                    statistics.mean([d["total_quantity"] for d in history]), 2
                ) if history else 0
            }
        }
    
    def generate_restock_report(
        self,
        warehouse_id: int,
        min_recommended_qty: int = 1
    ) -> Dict[str, Any]:
        """
        Generate a comprehensive restock report for a warehouse.
        
        Args:
            warehouse_id: Warehouse to analyze
            min_recommended_qty: Minimum recommended quantity to include
            
        Returns:
            Report dictionary with all recommendations
        """
        all_recommendations = self.predictor.predict_for_all_products(warehouse_id)
        
        # Filter to only items needing restock
        needing_restock = [
            r for r in all_recommendations 
            if r.recommended_quantity >= min_recommended_qty
        ]
        
        total_units_needed = sum(r.recommended_quantity for r in needing_restock)
        
        return {
            "generated_at": datetime.now().isoformat(),
            "warehouse_id": warehouse_id,
            "summary": {
                "total_products_analyzed": len(all_recommendations),
                "products_needing_restock": len(needing_restock),
                "total_units_to_restock": total_units_needed
            },
            "recommendations": [r.to_dict() for r in needing_restock],
            "low_stock_alerts": [
                r.to_dict() for r in all_recommendations 
                if r.days_of_stock <= 3 and r.recommended_quantity > 0
            ]
        }
