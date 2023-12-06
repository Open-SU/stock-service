package com.open.su.exceptions;

import io.grpc.Status;

public class ItemServiceException extends RuntimeException {

    /**
     * Predefined exception for database errors.
     */
    public static final ItemServiceException DATABASE_ERROR = new ItemServiceException(Type.DATABASE_ERROR, "Database error");

    /**
     * Predefined exception for not found errors.
     */
    public static final ItemServiceException NOT_FOUND = new ItemServiceException(Type.NOT_FOUND, "Not found");

    /**
     * Predefined exception for conflict errors.
     */
    public static final ItemServiceException CONFLICT = new ItemServiceException(Type.CONFLICT, "Conflict");

    /**
     * Predefined exception for invalid argument errors.
     */
    public static final ItemServiceException INVALID_ARGUMENT = new ItemServiceException(Type.INVALID_ARGUMENT, "Invalid argument");

    final Type type;

    ItemServiceException(Type type, String message) {
        super(message);
        this.type = type;
    }

    ItemServiceException(Type type, String message, Throwable cause) {
        super(message, cause);
        this.type = type;
    }

    public Type getType() {
        return type;
    }

    public ItemServiceException withMessage(String message) {
        return new ItemServiceException(type, message, this);
    }

    public ItemServiceException withCause(Throwable cause) {
        return new ItemServiceException(type, getMessage(), cause);
    }

    /**
     * Converts this exception to a {@link RuntimeException} that is gRPC suitable.
     *
     * @return the gRPC suitable exception
     */
    public RuntimeException toGrpcException() {
        return switch (type) {
            case DATABASE_ERROR ->
                    Status.INTERNAL.withDescription(getMessage()).withCause(getCause()).asRuntimeException();
            case NOT_FOUND -> Status.NOT_FOUND.withDescription(getMessage()).withCause(getCause()).asRuntimeException();
            case CONFLICT ->
                    Status.ALREADY_EXISTS.withDescription(getMessage()).withCause(getCause()).asRuntimeException();
            case INVALID_ARGUMENT ->
                    Status.INVALID_ARGUMENT.withDescription(getMessage()).withCause(getCause()).asRuntimeException();
        };
    }

    /**
     * Types of possible {@link ItemServiceException}.
     */
    public enum Type {
        DATABASE_ERROR,
        NOT_FOUND,
        CONFLICT,
        INVALID_ARGUMENT
    }
}
