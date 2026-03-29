package com.example.commons.exception;

/**
 * Exception thrown when a required object is null.
 */
public class NullObjectException extends RuntimeException {

    private final String objectName;

    public NullObjectException(String objectName) {
        super(objectName + " must not be null");
        this.objectName = objectName;
    }

    public NullObjectException(String objectName, String message) {
        super(message);
        this.objectName = objectName;
    }

    public String getObjectName() {
        return objectName;
    }
}
