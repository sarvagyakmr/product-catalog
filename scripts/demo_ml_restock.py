"""
Demo: ML-Based Restock Prediction

This script demonstrates the moving average-based restock prediction system.
It can work in two modes:
1. Generate synthetic order data and show predictions
2. Analyze existing data from the SQLite database

Usage:
    python demo_ml_restock.py --generate-data --days 30
    python demo_ml_restock.py --analyze --warehouse-id 1
"""

import argparse
import random
import logging
import sys
from datetime import datetime, timedelta
from typing import List, Dict, Any

from order_repository import OrderRepository
from restock_predictor import RestockAnalyzer, MovingAverageRestockPredictor

logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s - %(levelname)s - %(message)s",
    handlers=[logging.StreamHandler(sys.stdout)]
)
logger = logging.getLogger(__name__)


def generate_synthetic_orders(
    repository: OrderRepository,
    product_ids: List[int],
    warehouse_id: int = 1,
    days: int = 30,
    orders_per_day: int = 5
) -> None:
    """
    Generate synthetic order data for demonstration purposes.
    
    Args:
        repository: OrderRepository instance
        product_ids: List of product IDs to generate orders for
        warehouse_id: Warehouse ID
        days: Number of days of history to generate
        orders_per_day: Average orders per day
    """
    logger.info("Generating %d days of synthetic order data...", days)
    
    today = datetime.now()
    total_orders = 0
    
    # Generate orders for each day
    for day_offset in range(days, 0, -1):
        order_date = today - timedelta(days=day_offset)
        
        # Random number of orders for this day
        num_orders = random.randint(orders_per_day - 2, orders_per_day + 2)
        
        for order_idx in range(num_orders):
            # Create order data
            order_id = total_orders + 1
            
            # Random number of items (1-3)
            num_items = random.randint(1, 3)
            items = []
            
            for _ in range(num_items):
                product_id = random.choice(product_ids)
                quantity = random.randint(1, 10)
                items.append({
                    "productId": product_id,
                    "orderedQuantity": quantity,
                    "allocatedQuantity": quantity
                })
            
            order_data = {
                "id": order_id,
                "channelOrderId": f"DEMO-{order_date.strftime('%Y%m%d')}-{order_idx}",
                "channel": "INTERNAL",
                "warehouseId": warehouse_id,
                "status": "ALLOCATED",
                "items": items
            }
            
            # Save to repository (bypassing the date auto-assignment)
            with repository._get_connection() as conn:
                cursor = conn.cursor()
                
                for item in items:
                    cursor.execute("""
                        INSERT INTO order_items 
                        (order_id, channel_order_id, product_id, ordered_quantity, 
                         allocated_quantity, warehouse_id, channel, order_status, consumed_at)
                        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                    """, (
                        order_id,
                        order_data["channelOrderId"],
                        item["productId"],
                        item["orderedQuantity"],
                        item["allocatedQuantity"],
                        warehouse_id,
                        "INTERNAL",
                        "ALLOCATED",
                        order_date.isoformat()
                    ))
                
                # Update daily stats
                product_stats: Dict[int, Dict[str, Any]] = {}
                for item in items:
                    pid = item["productId"]
                    if pid not in product_stats:
                        product_stats[pid] = {"count": 0, "quantity": 0}
                    product_stats[pid]["count"] += 1
                    product_stats[pid]["quantity"] += item["allocatedQuantity"]
                
                for pid, stats in product_stats.items():
                    cursor.execute("""
                        SELECT total_orders, total_quantity FROM daily_product_stats
                        WHERE product_id = ? AND warehouse_id = ? AND order_date = ?
                    """, (pid, warehouse_id, order_date.date().isoformat()))
                    
                    row = cursor.fetchone()
                    if row:
                        new_orders = row["total_orders"] + stats["count"]
                        new_quantity = row["total_quantity"] + stats["quantity"]
                        avg_size = new_quantity / new_orders if new_orders > 0 else 0
                        
                        cursor.execute("""
                            UPDATE daily_product_stats
                            SET total_orders = ?, total_quantity = ?, avg_order_size = ?
                            WHERE product_id = ? AND warehouse_id = ? AND order_date = ?
                        """, (new_orders, new_quantity, avg_size, pid, warehouse_id, order_date.date().isoformat()))
                    else:
                        avg_size = stats["quantity"] / stats["count"] if stats["count"] > 0 else 0
                        cursor.execute("""
                            INSERT INTO daily_product_stats
                            (product_id, warehouse_id, order_date, total_orders, total_quantity, avg_order_size)
                            VALUES (?, ?, ?, ?, ?, ?)
                        """, (pid, warehouse_id, order_date.date().isoformat(), stats["count"], stats["quantity"], avg_size))
            
            total_orders += 1
    
    logger.info("Generated %d synthetic orders", total_orders)


def demonstrate_ml_features(db_path: str, warehouse_id: int = 1) -> None:
    """
    Demonstrate the ML restock prediction features.
    
    Args:
        db_path: Path to SQLite database
        warehouse_id: Warehouse ID to analyze
    """
    analyzer = RestockAnalyzer(db_path)
    
    print("\n" + "=" * 70)
    print("ML RESTOCK PREDICTION DEMONSTRATION")
    print("=" * 70)
    
    # Get summary stats
    summary = analyzer.repository.get_summary_stats()
    print(f"\n📊 Database Summary:")
    print(f"  Total order items: {summary['total_order_items']}")
    print(f"  Unique orders: {summary['total_unique_orders']}")
    print(f"  Unique products: {summary['total_unique_products']}")
    
    # Generate restock report
    print(f"\n📈 Generating Restock Report for Warehouse {warehouse_id}...")
    report = analyzer.generate_restock_report(warehouse_id, min_recommended_qty=1)
    
    print(f"\n  Products analyzed: {report['summary']['total_products_analyzed']}")
    print(f"  Products needing restock: {report['summary']['products_needing_restock']}")
    print(f"  Total units to restock: {report['summary']['total_units_to_restock']}")
    
    if report['recommendations']:
        print(f"\n🔝 Top 10 Restock Recommendations:")
        print("-" * 70)
        print(f"{'Rank':<6} {'Product':<10} {'Qty':<8} {'Confidence':<12} {'Days Stock':<12} {'Method':<20}")
        print("-" * 70)
        
        for i, rec in enumerate(report['recommendations'][:10], 1):
            print(
                f"{i:<6} "
                f"{rec['product_id']:<10} "
                f"{rec['recommended_quantity']:<8} "
                f"{rec['confidence_score']:<12.2f} "
                f"{rec['days_of_stock']:<12} "
                f"{rec['prediction_method']:<20}"
            )
    
    # Show detailed analysis for top product
    if report['recommendations']:
        top_product_id = report['recommendations'][0]['product_id']
        print(f"\n📋 Detailed Analysis for Product {top_product_id}:")
        print("-" * 70)
        
        analysis = analyzer.analyze_product(top_product_id, warehouse_id, current_stock=50)
        
        rec = analysis['recommendation']
        print(f"  Recommended quantity: {rec['recommended_quantity']} units")
        print(f"  Average daily demand: {rec['avg_daily_demand']:.2f} units/day")
        print(f"  Demand volatility: {rec['demand_volatility']:.2f}")
        print(f"  Days of stock remaining: {rec['days_of_stock']}")
        print(f"  Confidence score: {rec['confidence_score']:.2f}")
        print(f"  Prediction method: {rec['prediction_method']}")
        
        print(f"\n  14-Day Demand Forecast:")
        for forecast in analysis['demand_forecast_14d'][:7]:
            print(f"    {forecast['date']}: {forecast['forecasted_demand']:.1f} units")
    
    print("\n" + "=" * 70)


def main():
    parser = argparse.ArgumentParser(
        description="Demo: ML-Based Restock Prediction System"
    )
    parser.add_argument(
        "--db-path",
        default="orders_history.db",
        help="Path to SQLite database (default: orders_history.db)"
    )
    parser.add_argument(
        "--warehouse-id",
        type=int,
        default=1,
        help="Warehouse ID (default: 1)"
    )
    parser.add_argument(
        "--generate-data",
        action="store_true",
        help="Generate synthetic order data for demo"
    )
    parser.add_argument(
        "--analyze",
        action="store_true",
        help="Run ML analysis on existing data"
    )
    parser.add_argument(
        "--days",
        type=int,
        default=30,
        help="Days of synthetic data to generate (default: 30)"
    )
    parser.add_argument(
        "--products",
        type=int,
        nargs="+",
        default=[1, 2, 3, 4, 5],
        help="Product IDs to generate data for (default: 1 2 3 4 5)"
    )
    
    args = parser.parse_args()
    
    if not args.generate_data and not args.analyze:
        # Default: do both
        args.generate_data = True
        args.analyze = True
    
    if args.generate_data:
        repository = OrderRepository(args.db_path)
        generate_synthetic_orders(
            repository,
            args.products,
            args.warehouse_id,
            args.days
        )
    
    if args.analyze:
        demonstrate_ml_features(args.db_path, args.warehouse_id)


if __name__ == "__main__":
    main()
