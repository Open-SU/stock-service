package com.open.su.controllers.models;

import java.util.UUID;

public record IncrementStockErrorMessage(Throwable throwable, Type type, UUID itemId) {
    public enum Type {
        ITEM_NOT_FOUND,
        INVALID_QUANTITY,
        UNEXPECTED_ERROR
    }
}
