package com.example.commons.service;

import com.example.commons.exception.NullObjectException;

/**
 * Abstract base service providing common utility methods for all services.
 */
public abstract class AbstractService {

    /**
     * Checks if the given object is null. If so, throws a {@link NullObjectException}.
     *
     * @param obj     the object to check
     * @param message the message or object name for the exception
     * @param <T>     the type of the object
     * @return the object if not null
     * @throws NullObjectException if the object is null
     */
    protected <T> T checkNotNull(T obj, String message) {
        if (obj == null) {
            throw new NullObjectException(message != null ? message : "Object");
        }
        return obj;
    }

    /**
     * Checks if the given object is null. If so, throws a {@link NullObjectException}
     * with the provided object name.
     *
     * @param obj        the object to check
     * @param objectName the name of the object for the exception message
     * @param <T>        the type of the object
     * @return the object if not null
     * @throws NullObjectException if the object is null
     */
    protected <T> T checkNotNull(T obj, String objectName, String customMessage) {
        if (obj == null) {
            throw new NullObjectException(objectName, customMessage != null ? customMessage : objectName + " must not be null");
        }
        return obj;
    }
}
