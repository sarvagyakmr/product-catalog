"""
Mock Product Generator Module

This module provides functionality to generate and create mock products
in the Product Catalog service using Faker for realistic fake data,
and to create and manage inventory records for those products.
"""

import logging
from typing import List

import requests
from faker import Faker

# Configure logging
logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s - %(levelname)s - %(message)s"
)
logger = logging.getLogger(__name__)

# API Configuration
DEFAULT_PRODUCT_CATALOG_URL = "http://localhost:8080"
DEFAULT_INVENTORY_URL = "http://localhost:8081"
DEFAULT_ORDER_MANAGEMENT_URL = "http://localhost:8082"

PRODUCTS_ENDPOINT = "/api/products"
INVENTORY_ENDPOINT = "/api/inventory"
INVENTORY_MOVE_ENDPOINT = "/api/inventory/move"
OUTWARD_ORDERS_ENDPOINT = "/api/outward-orders"

# Valid enum values for Product API
PRODUCT_TYPES = ["SINGLE", "COMBO"]
PACK_TYPES = ["EACH", "BOX"]

# Inventory states
INVENTORY_STATE_RECEIVED = "RECEIVED"
INVENTORY_STATE_AVAILABLE = "AVAILABLE"


def create_mock_product(
    fake: Faker,
    base_url: str = DEFAULT_PRODUCT_CATALOG_URL,
    client_id: int = 1
) -> int:
    """
    Create a single mock product via the Product Catalog API.

    Args:
        fake: Faker instance for generating fake data.
        base_url: Base URL of the Product Catalog service.
        client_id: Client ID to associate with the product.

    Returns:
        The productId (integer) returned by the API after creation.

    Raises:
        requests.HTTPError: If the API request fails.
    """
    product_data = {
        "clientId": client_id,
        "skuId": fake.unique.ean(length=13),  # Generate unique EAN-13 as SKU
        "type": fake.random_element(elements=PRODUCT_TYPES),
        "packType": fake.random_element(elements=PACK_TYPES),
    }

    url = f"{base_url}{PRODUCTS_ENDPOINT}"
    response = requests.post(url, json=product_data, timeout=10)
    response.raise_for_status()

    created_product = response.json()
    product_id = created_product.get("id")

    logger.info("Created product with productId: %s (skuId=%s, type=%s, packType=%s)",
                product_id, product_data["skuId"], product_data["type"], product_data["packType"])

    return product_id


def generate_mock_products(
    count: int = 100,
    base_url: str = DEFAULT_PRODUCT_CATALOG_URL,
    client_id: int = 1
) -> List[int]:
    """
    Generate and create a specified number of mock products in the Product Catalog.

    Args:
        count: Number of mock products to generate (default: 100).
        base_url: Base URL of the Product Catalog service (default: localhost:8080).
        client_id: Client ID to associate with all products (default: 1).

    Returns:
        A list of generated productIds (integers).

    Example:
        >>> product_ids = generate_mock_products(100)
        >>> print(f"Created {len(product_ids)} products")
    """
    fake = Faker()
    product_ids: List[int] = []

    logger.info("Starting generation of %d mock products...", count)

    for i in range(count):
        try:
            product_id = create_mock_product(fake, base_url, client_id)
            product_ids.append(product_id)
        except requests.RequestException as e:
            logger.error("Failed to create product %d/%d: %s", i + 1, count, e)

    logger.info("Completed generating mock products. Total created: %d", len(product_ids))
    return product_ids


# =============================================================================
# Inventory Management Functions
# =============================================================================

def create_inventory_record(
    product_id: int,
    quantity: int,
    warehouse_id: int,
    base_url: str = DEFAULT_INVENTORY_URL
) -> int:
    """
    Create an initial inventory record for a product.

    The inventory is created in the RECEIVED state.

    Args:
        product_id: The product ID to create inventory for.
        quantity: Quantity to add to inventory.
        warehouse_id: Warehouse ID where inventory is located.
        base_url: Base URL of the Inventory service (default: localhost:8081).

    Returns:
        The inventory record ID returned by the API.

    Raises:
        requests.HTTPError: If the API request fails.
    """
    payload = {
        "productId": product_id,
        "quantity": quantity,
        "warehouseId": warehouse_id,
    }

    url = f"{base_url}{INVENTORY_ENDPOINT}"
    response = requests.post(url, json=payload, timeout=10)
    response.raise_for_status()

    created_inventory = response.json()
    inventory_id = created_inventory.get("id")

    logger.info(
        "Created inventory record id=%s for productId=%s (quantity=%d, warehouseId=%d) in RECEIVED state",
        inventory_id, product_id, quantity, warehouse_id
    )

    return inventory_id


def move_inventory_to_available(
    product_id: int,
    quantity: int,
    warehouse_id: int,
    base_url: str = DEFAULT_INVENTORY_URL
) -> None:
    """
    Transition inventory for a product from RECEIVED to AVAILABLE state.

    Args:
        product_id: The product ID whose inventory to move.
        quantity: Quantity to move.
        warehouse_id: Warehouse ID.
        base_url: Base URL of the Inventory service (default: localhost:8081).

    Raises:
        requests.HTTPError: If the API request fails.
    """
    payload = {
        "productId": product_id,
        "fromState": INVENTORY_STATE_RECEIVED,
        "toState": INVENTORY_STATE_AVAILABLE,
        "quantity": quantity,
        "warehouseId": warehouse_id,
    }

    url = f"{base_url}{INVENTORY_MOVE_ENDPOINT}"
    response = requests.post(url, json=payload, timeout=10)
    response.raise_for_status()

    logger.info(
        "Moved productId=%s: RECEIVED -> AVAILABLE (quantity=%d, warehouseId=%d)",
        product_id, quantity, warehouse_id
    )


def create_and_activate_inventory_for_products(
    product_ids: List[int],
    quantity: int = 100,
    warehouse_id: int = 1,
    inventory_base_url: str = DEFAULT_INVENTORY_URL
) -> None:
    """
    For each product ID, create an inventory record and transition it to AVAILABLE state.

    This function:
      1. Creates inventory for each product in RECEIVED state
      2. Moves all inventory from RECEIVED to AVAILABLE

    Args:
        product_ids: List of product IDs to create inventory for.
        quantity: Quantity per product (default: 100).
        warehouse_id: Warehouse ID to use (default: 1).
        inventory_base_url: Base URL of the Inventory service (default: localhost:8081).

    Example:
        >>> product_ids = generate_mock_products(10)
        >>> create_and_activate_inventory_for_products(product_ids)
    """
    logger.info(
        "Creating inventory for %d products (quantity=%d, warehouseId=%d)...",
        len(product_ids), quantity, warehouse_id
    )

    # Step 1: Create inventory records (all start in RECEIVED state)
    for i, pid in enumerate(product_ids, start=1):
        try:
            create_inventory_record(pid, quantity, warehouse_id, inventory_base_url)
        except requests.RequestException as e:
            logger.error("Failed to create inventory for productId=%s: %s", pid, e)

    logger.info("All inventory records created. Now transitioning to AVAILABLE state...")

    # Step 2: Move all from RECEIVED -> AVAILABLE
    for i, pid in enumerate(product_ids, start=1):
        try:
            move_inventory_to_available(pid, quantity, warehouse_id, inventory_base_url)
        except requests.RequestException as e:
            logger.error("Failed to move inventory for productId=%s to AVAILABLE: %s", pid, e)

    logger.info(
        "Completed inventory setup for %d products. All items now in AVAILABLE state.",
        len(product_ids)
    )


# =============================================================================
# Outward Order Functions
# =============================================================================

def fetch_products(
    base_url: str = DEFAULT_PRODUCT_CATALOG_URL
) -> List[dict]:
    """
    Fetch all products from the Product Catalog.

    Args:
        base_url: Base URL of the Product Catalog service.

    Returns:
        List of product dicts (each has 'id', 'skuId', 'type', 'packType', etc.).
    """
    url = f"{base_url}{PRODUCTS_ENDPOINT}"
    response = requests.get(url, timeout=10)
    response.raise_for_status()
    return response.json()


def create_outward_order(
    channel_order_id: str,
    channel: str,
    warehouse_id: int,
    items: List[dict],
    base_url: str = DEFAULT_ORDER_MANAGEMENT_URL
) -> int:
    """
    Create a single outward order via the Order Management API.

    Args:
        channel_order_id: Unique external order ID (e.g., from a sales channel).
        channel: Channel name (e.g., "INTERNAL").
        warehouse_id: Warehouse to fulfill from.
        items: List of {"productId": int, "quantity": int} dicts.
        base_url: Base URL of the Order Management service.

    Returns:
        The created order's ID.

    Raises:
        requests.HTTPError: If the API request fails.
    """
    payload = {
        "channelOrderId": channel_order_id,
        "channel": channel,
        "warehouseId": warehouse_id,
        "items": items,
    }

    url = f"{base_url}{OUTWARD_ORDERS_ENDPOINT}"
    response = requests.post(url, json=payload, timeout=10)
    response.raise_for_status()

    created = response.json()
    order_id = created.get("id")

    logger.info(
        "Created outward order id=%s channelOrderId=%s with %d item(s)",
        order_id, channel_order_id, len(items)
    )
    for item in items:
        logger.info("  - productId=%s quantity=%s", item["productId"], item["quantity"])

    return order_id


def place_random_outward_orders(
    num_orders: int = 10,
    min_items_per_order: int = 1,
    max_items_per_order: int = 5,
    min_qty: int = 1,
    max_qty: int = 10,
    warehouse_id: int = 1,
    channel: str = "INTERNAL",
    product_base_url: str = DEFAULT_PRODUCT_CATALOG_URL,
    order_base_url: str = DEFAULT_ORDER_MANAGEMENT_URL
) -> List[int]:
    """
    Randomly select products and place varying quantities of outward orders.

    This function:
      1. Fetches available products from the Product Catalog
      2. For each of `num_orders` orders:
         - Randomly selects N products (between min_items_per_order and max_items_per_order)
         - Assigns random quantities (between min_qty and max_qty) to each item
         - Creates the outward order via the Order Management API

    Args:
        num_orders: Number of orders to create (default: 10).
        min_items_per_order: Minimum products per order (default: 1).
        max_items_per_order: Maximum products per order (default: 5).
        min_qty: Minimum quantity per item (default: 1).
        max_qty: Maximum quantity per item (default: 10).
        warehouse_id: Warehouse ID for orders (default: 1).
        channel: Sales channel (default: "INTERNAL").
        product_base_url: Base URL of Product Catalog.
        order_base_url: Base URL of Order Management.

    Returns:
        List of created order IDs.

    Example:
        >>> order_ids = place_random_outward_orders(num_orders=20)
        >>> print(f"Placed {len(order_ids)} orders")
    """
    fake = Faker()

    # Step 1: Fetch products
    logger.info("Fetching products from Product Catalog...")
    products = fetch_products(product_base_url)
    if not products:
        logger.warning("No products found in catalog. Cannot place orders.")
        return []

    product_ids = [p["id"] for p in products]
    logger.info("Found %d products in catalog.", len(product_ids))

    order_ids: List[int] = []

    logger.info(
        "Placing %d random outward orders (items: %d-%d, qty: %d-%d)...",
        num_orders, min_items_per_order, max_items_per_order, min_qty, max_qty
    )

    for i in range(num_orders):
        try:
            # Randomly select number of items for this order
            num_items = fake.random_int(min=min_items_per_order, max=max_items_per_order)
            # Sample unique product IDs
            selected_ids = fake.random_elements(elements=product_ids, length=num_items, unique=True)

            # Build items with random quantities
            items = []
            for pid in selected_ids:
                qty = fake.random_int(min=min_qty, max=max_qty)
                items.append({"productId": pid, "quantity": qty})

            # Generate a unique channelOrderId
            channel_order_id = f"ORD-{fake.uuid4()[:8].upper()}"

            order_id = create_outward_order(
                channel_order_id=channel_order_id,
                channel=channel,
                warehouse_id=warehouse_id,
                items=items,
                base_url=order_base_url
            )
            order_ids.append(order_id)

        except requests.RequestException as e:
            logger.error("Failed to create order %d/%d: %s", i + 1, num_orders, e)

    logger.info("Completed placing %d outward orders.", len(order_ids))
    return order_ids


if __name__ == "__main__":
    # Run standalone: generate 100 mock products, create inventory, and activate
    ids = generate_mock_products(count=100)
    logger.info("Generated productIds: %s", ids)

    # Create inventory and move to available state
    create_and_activate_inventory_for_products(ids)

    # Place random outward orders
    placed = place_random_outward_orders(num_orders=10)
    logger.info("Placed orderIds: %s", placed)
