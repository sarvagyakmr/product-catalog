CREATE TABLE IF NOT EXISTS products (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    client_id BIGINT NOT NULL,
    sku_id VARCHAR(255) NOT NULL,
    type VARCHAR(50) NOT NULL,
    pack_type VARCHAR(50),
    version INT,
    created_at TIMESTAMP,
    updated_at TIMESTAMP
);

CREATE TABLE IF NOT EXISTS product_variants (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    product_id BIGINT NOT NULL,
    variant_product_id BIGINT NOT NULL,
    variant_type VARCHAR(50) NOT NULL
);

CREATE TABLE IF NOT EXISTS combo_products (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    combo_product_id BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    quantity INT NOT NULL
);

CREATE TABLE IF NOT EXISTS pack_conversions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    sku_id VARCHAR(255) NOT NULL,
    from_pack_type VARCHAR(50) NOT NULL,
    to_pack_type VARCHAR(50) NOT NULL,
    conversion_factor INT NOT NULL
);



