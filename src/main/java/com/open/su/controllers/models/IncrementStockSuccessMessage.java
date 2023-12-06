package com.open.su.controllers.models;

import java.util.UUID;

public record IncrementStockSuccessMessage(UUID itemId, Long stock) {
}
