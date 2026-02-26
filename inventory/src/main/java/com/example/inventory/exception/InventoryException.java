package com.example.inventory.exception;

public class InventoryException extends RuntimeException {

    private final InventoryErrorCode errorCode;

    public InventoryException(InventoryErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public InventoryErrorCode getErrorCode() {
        return errorCode;
    }

    public enum InventoryErrorCode {
        INVALID_STATE_TRANSITION,
        INSUFFICIENT_QUANTITY,
        INVENTORY_NOT_FOUND,
        PRODUCT_NOT_FOUND,
        COMBO_DEFINITION_NOT_FOUND
    }
}
