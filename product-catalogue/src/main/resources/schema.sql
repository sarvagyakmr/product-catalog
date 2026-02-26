CREATE TABLE IF NOT EXISTS products (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    client_id BIGINT NOT NULL,
    sku_id VARCHAR(255) NOT NULL,
    type VARCHAR(50) NOT NULL,
    pack_size INT
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



