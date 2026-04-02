"""
Order Repository Module

Provides SQLite storage for historical order items consumed from Redis.
Used for analytics and ML-based restock prediction.
"""

import sqlite3
import json
import logging
from datetime import datetime
from typing import List, Dict, Any, Optional, Tuple
from contextlib import contextmanager
from dataclasses import dataclass

logger = logging.getLogger(__name__)

DEFAULT_DB_PATH = "orders_history.db"


@dataclass
class OrderItemRecord:
    """Represents a historical order item record."""
    id: Optional[int]
    order_id: int
    channel_order_id: str
    product_id: int
    ordered_quantity: int
    allocated_quantity: int
    warehouse_id: int
    channel: str
    order_status: str
    consumed_at: datetime


class OrderRepository:
    """
    SQLite repository for storing historical order items.
    
    Provides methods to persist order items from Redis events and
    query historical data for analytics and ML predictions.
    """
    
    def __init__(self, db_path: str = DEFAULT_DB_PATH):
        self.db_path = db_path
        self._init_database()
    
    @contextmanager
    def _get_connection(self):
        """Context manager for database connections."""
        conn = sqlite3.connect(self.db_path)
        conn.row_factory = sqlite3.Row
        try:
            yield conn
            conn.commit()
        except Exception:
            conn.rollback()
            raise
        finally:
            conn.close()
    
    def _init_database(self) -> None:
        """Initialize the SQLite database with required tables."""
        with self._get_connection() as conn:
            cursor = conn.cursor()
            
            # Main table for order items
            cursor.execute("""
                CREATE TABLE IF NOT EXISTS order_items (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    order_id INTEGER NOT NULL,
                    channel_order_id TEXT NOT NULL,
                    product_id INTEGER NOT NULL,
                    ordered_quantity INTEGER NOT NULL,
                    allocated_quantity INTEGER NOT NULL,
                    warehouse_id INTEGER NOT NULL,
                    channel TEXT NOT NULL,
                    order_status TEXT NOT NULL,
                    consumed_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                )
            """)
            
            # Indexes for efficient querying
            cursor.execute("""
                CREATE INDEX IF NOT EXISTS idx_product_id 
                ON order_items(product_id)
            """)
            cursor.execute("""
                CREATE INDEX IF NOT EXISTS idx_consumed_at 
                ON order_items(consumed_at)
            """)
            cursor.execute("""
                CREATE INDEX IF NOT EXISTS idx_product_warehouse 
                ON order_items(product_id, warehouse_id)
            """)
            cursor.execute("""
                CREATE INDEX IF NOT EXISTS idx_product_time 
                ON order_items(product_id, consumed_at)
            """)
            
            # Aggregated daily stats table (for faster ML queries)
            cursor.execute("""
                CREATE TABLE IF NOT EXISTS daily_product_stats (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    product_id INTEGER NOT NULL,
                    warehouse_id INTEGER NOT NULL,
                    order_date DATE NOT NULL,
                    total_orders INTEGER DEFAULT 0,
                    total_quantity INTEGER DEFAULT 0,
                    avg_order_size REAL DEFAULT 0.0,
                    UNIQUE(product_id, warehouse_id, order_date)
                )
            """)
            
            cursor.execute("""
                CREATE INDEX IF NOT EXISTS idx_daily_stats_lookup 
                ON daily_product_stats(product_id, warehouse_id, order_date)
            """)
            
            logger.info("Database initialized at: %s", self.db_path)
    
    def save_order_items(self, order_data: Dict[str, Any]) -> int:
        """
        Save order items from a Redis order event.
        
        Args:
            order_data: Parsed JSON order data from Redis
            
        Returns:
            Number of items saved
        """
        order_id = order_data.get("id")
        channel_order_id = order_data.get("channelOrderId", "")
        channel = order_data.get("channel", "")
        warehouse_id = order_data.get("warehouseId")
        status = order_data.get("status", "")
        items = order_data.get("items", [])
        
        if not items:
            logger.warning("No items to save for order %s", order_id)
            return 0
        
        with self._get_connection() as conn:
            cursor = conn.cursor()
            saved_count = 0
            
            for item in items:
                cursor.execute("""
                    INSERT INTO order_items 
                    (order_id, channel_order_id, product_id, ordered_quantity, 
                     allocated_quantity, warehouse_id, channel, order_status)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                """, (
                    order_id,
                    channel_order_id,
                    item.get("productId"),
                    item.get("orderedQuantity", 0),
                    item.get("allocatedQuantity", 0),
                    warehouse_id,
                    channel,
                    status
                ))
                saved_count += 1
            
            # Update daily stats
            self._update_daily_stats(conn, order_data)
            
            logger.debug("Saved %d items for order %s", saved_count, order_id)
            return saved_count
    
    def _update_daily_stats(self, conn: sqlite3.Connection, order_data: Dict[str, Any]) -> None:
        """Update aggregated daily statistics for efficient ML queries."""
        today = datetime.now().date().isoformat()
        warehouse_id = order_data.get("warehouseId", 0)
        items = order_data.get("items", [])
        
        cursor = conn.cursor()
        
        # Group items by product
        product_stats: Dict[int, Dict[str, Any]] = {}
        for item in items:
            pid = item.get("productId")
            if pid not in product_stats:
                product_stats[pid] = {"count": 0, "quantity": 0}
            product_stats[pid]["count"] += 1
            product_stats[pid]["quantity"] += item.get("allocatedQuantity", 0) or item.get("orderedQuantity", 0)
        
        # Update or insert daily stats
        for pid, stats in product_stats.items():
            cursor.execute("""
                SELECT total_orders, total_quantity FROM daily_product_stats
                WHERE product_id = ? AND warehouse_id = ? AND order_date = ?
            """, (pid, warehouse_id, today))
            
            row = cursor.fetchone()
            if row:
                new_orders = row["total_orders"] + stats["count"]
                new_quantity = row["total_quantity"] + stats["quantity"]
                avg_size = new_quantity / new_orders if new_orders > 0 else 0
                
                cursor.execute("""
                    UPDATE daily_product_stats
                    SET total_orders = ?, total_quantity = ?, avg_order_size = ?
                    WHERE product_id = ? AND warehouse_id = ? AND order_date = ?
                """, (new_orders, new_quantity, avg_size, pid, warehouse_id, today))
            else:
                avg_size = stats["quantity"] / stats["count"] if stats["count"] > 0 else 0
                cursor.execute("""
                    INSERT INTO daily_product_stats
                    (product_id, warehouse_id, order_date, total_orders, total_quantity, avg_order_size)
                    VALUES (?, ?, ?, ?, ?, ?)
                """, (pid, warehouse_id, today, stats["count"], stats["quantity"], avg_size))
    
    def get_product_order_history(
        self, 
        product_id: int, 
        warehouse_id: Optional[int] = None,
        days: int = 30
    ) -> List[OrderItemRecord]:
        """
        Get historical order items for a specific product.
        
        Args:
            product_id: Product ID to query
            warehouse_id: Optional warehouse filter
            days: Number of days to look back
            
        Returns:
            List of OrderItemRecord objects
        """
        with self._get_connection() as conn:
            cursor = conn.cursor()
            
            if warehouse_id:
                cursor.execute("""
                    SELECT * FROM order_items
                    WHERE product_id = ? AND warehouse_id = ?
                    AND consumed_at >= datetime('now', '-{} days')
                    ORDER BY consumed_at DESC
                """.format(days), (product_id, warehouse_id))
            else:
                cursor.execute("""
                    SELECT * FROM order_items
                    WHERE product_id = ?
                    AND consumed_at >= datetime('now', '-{} days')
                    ORDER BY consumed_at DESC
                """.format(days), (product_id,))
            
            rows = cursor.fetchall()
            return [self._row_to_record(row) for row in rows]
    
    def get_daily_stats(
        self,
        product_id: int,
        warehouse_id: int,
        days: int = 30
    ) -> List[Dict[str, Any]]:
        """
        Get daily aggregated statistics for a product.
        
        Args:
            product_id: Product ID to query
            warehouse_id: Warehouse ID
            days: Number of days to look back
            
        Returns:
            List of daily stats dictionaries
        """
        with self._get_connection() as conn:
            cursor = conn.cursor()
            cursor.execute("""
                SELECT * FROM daily_product_stats
                WHERE product_id = ? AND warehouse_id = ?
                AND order_date >= date('now', '-{} days')
                ORDER BY order_date ASC
            """.format(days), (product_id, warehouse_id))
            
            return [dict(row) for row in cursor.fetchall()]
    
    def get_all_products(self) -> List[int]:
        """Get list of all unique product IDs in the database."""
        with self._get_connection() as conn:
            cursor = conn.cursor()
            cursor.execute("SELECT DISTINCT product_id FROM order_items")
            return [row[0] for row in cursor.fetchall()]
    
    def get_summary_stats(self) -> Dict[str, Any]:
        """Get summary statistics of the database."""
        with self._get_connection() as conn:
            cursor = conn.cursor()
            
            cursor.execute("SELECT COUNT(*) FROM order_items")
            total_items = cursor.fetchone()[0]
            
            cursor.execute("SELECT COUNT(DISTINCT order_id) FROM order_items")
            total_orders = cursor.fetchone()[0]
            
            cursor.execute("SELECT COUNT(DISTINCT product_id) FROM order_items")
            total_products = cursor.fetchone()[0]
            
            cursor.execute("SELECT MIN(consumed_at), MAX(consumed_at) FROM order_items")
            date_range = cursor.fetchone()
            
            return {
                "total_order_items": total_items,
                "total_unique_orders": total_orders,
                "total_unique_products": total_products,
                "date_range": {
                    "earliest": date_range[0],
                    "latest": date_range[1]
                }
            }
    
    def _row_to_record(self, row: sqlite3.Row) -> OrderItemRecord:
        """Convert a database row to an OrderItemRecord."""
        return OrderItemRecord(
            id=row["id"],
            order_id=row["order_id"],
            channel_order_id=row["channel_order_id"],
            product_id=row["product_id"],
            ordered_quantity=row["ordered_quantity"],
            allocated_quantity=row["allocated_quantity"],
            warehouse_id=row["warehouse_id"],
            channel=row["channel"],
            order_status=row["order_status"],
            consumed_at=datetime.fromisoformat(row["consumed_at"])
        )
