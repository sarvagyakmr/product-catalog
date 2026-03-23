package com.example.commons.constants;

public final class ErrorMessages {

    private ErrorMessages() {
        // Prevent instantiation
    }

    // Product related errors
    public static final String PRODUCT_NOT_FOUND_WITH_ID = "Product does not exist with ID: %s";
    public static final String PRODUCT_NOT_FOUND_WITH_SKU_AND_PACK_TYPE = "Product does not exist with sku: %s and pack type: %s";
    public static final String COMPONENT_PRODUCT_NOT_FOUND_WITH_ID = "Component product not found with ID: %s";

    // Combo related errors
    public static final String COMBO_DEFINITION_NOT_FOUND = "Combo definition not found for product ID: %s";

    // Pack conversion related errors
    public static final String PACK_CONVERSION_NOT_FOUND = "Pack conversion not found for sku %s from %s to %s";
    public static final String PACK_CONVERSION_SAME_TYPE = "From and to pack types must be different";
    public static final String PACK_TYPE_MISMATCH_FROM = "Product pack type %s does not match from pack type %s";
    public static final String PACK_TYPE_MISMATCH_TO = "Product pack type %s does not match to pack type %s";
    public static final String COMPONENT_PACK_TYPE_MISMATCH = "Component product pack type %s does not match from pack type %s";
    public static final String CONVERSION_FACTOR_POSITIVE = "Conversion factor must be positive";
    public static final String CONVERTED_QUANTITY_EXCEEDS_RANGE = "Converted quantity exceeds supported range";

    // Quantity related errors
    public static final String QUANTITY_MUST_BE_POSITIVE = "Quantity to move must be positive";
    public static final String QUANTITY_TO_CONVERT_MUST_BE_POSITIVE = "Quantity to convert must be positive";
    public static final String INSUFFICIENT_QUANTITY_WITH_STATE = "Insufficient quantity for product %s in %s state. Available: %s, Requested: %s";
    public static final String INSUFFICIENT_QUANTITY_AVAILABLE = "Insufficient quantity for product %s in AVAILABLE state. Available: %s, Requested: %s";

    // Inventory related errors
    public static final String INVENTORY_NOT_FOUND_WITH_STATE = "No inventory found for product %s in state %s with pack type %s";
    public static final String INVENTORY_NOT_FOUND_AVAILABLE = "No inventory found for product %s in AVAILABLE state with pack type %s";

    // State transition errors
    public static final String COMPLETE_STATE_TERMINAL = "Cannot move inventory from COMPLETE state — it is a terminal state";
    public static final String INVALID_STATE_TRANSITION = "Invalid state transition from %s to %s. Allowed: AVAILABLE→ALLOCATED, ALLOCATED→COMPLETE, ALLOCATED→AVAILABLE";

    // Catalog client errors
    public static final String FAILED_TO_FETCH_PRODUCT = "Failed to fetch product from catalog";
    public static final String FAILED_TO_FETCH_COMBO_PRODUCT = "Failed to fetch combo product from catalog";
    public static final String FAILED_TO_FETCH_PACK_CONVERSION = "Failed to fetch pack conversion from catalog";

    // Generic API errors
    public static final String INVALID_REQUEST = "Invalid request";
    public static final String INVALID_PARAMETER = "Invalid parameter";
    public static final String MALFORMED_REQUEST_BODY = "Malformed or missing request body";
    public static final String INTERNAL_ERROR = "An unexpected error occurred";
}
