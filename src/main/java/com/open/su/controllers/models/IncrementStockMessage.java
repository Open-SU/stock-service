package com.open.su.controllers.models;

import java.util.UUID;

/**
 * Message received from the message queue.
 *
 * @see com.open.su.controllers.ItemMqpController
 */
public record IncrementStockMessage(UUID itemId, Long quantity) {
}
