package com.open.su.controllers;

import com.open.su.ItemService;
import com.open.su.controllers.models.IncrementStockErrorMessage;
import com.open.su.controllers.models.IncrementStockMessage;
import com.open.su.controllers.models.IncrementStockSuccessMessage;
import com.open.su.exceptions.ItemServiceException;
import io.smallrye.mutiny.Uni;
import io.smallrye.reactive.messaging.Targeted;
import io.vertx.core.json.JsonObject;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.eclipse.microprofile.reactive.messaging.Outgoing;
import org.jboss.logging.Logger;

@ApplicationScoped
public class ItemMqpController {
    private static final Logger LOGGER = Logger.getLogger(ItemMqpController.class);
    private final ItemService itemService;

    @Inject
    public ItemMqpController(ItemService itemService) {
        this.itemService = itemService;
    }

    /**
     * Update stock of an item
     *
     * @param message the message
     * @return a {@link Uni} of {@link Void}
     */
    @Incoming("increment-stock-in")
    @Outgoing("increment-stock-out")
    @Outgoing("increment-stock-error")
    public Uni<Targeted> incrementStock(JsonObject message) {
        IncrementStockMessage incrementStockMessage = message.mapTo(IncrementStockMessage.class);
        return itemService.incrementItemStock(incrementStockMessage.itemId(), incrementStockMessage.quantity())
                .onItem().transformToUni(t -> Uni.createFrom().item(Targeted.of("increment-stock-out", new IncrementStockSuccessMessage(t.getItem1(), t.getItem2()))))
                .onFailure().recoverWithUni(t -> {
                    final String errorExchangeName = "increment-stock-error";
                    if (t instanceof ItemServiceException itemServiceException) {
                        return switch (itemServiceException.getType()) {
                            case NOT_FOUND ->
                                    Uni.createFrom().item(Targeted.of(errorExchangeName, new IncrementStockErrorMessage(t, IncrementStockErrorMessage.Type.ITEM_NOT_FOUND, incrementStockMessage.itemId())));
                            case INVALID_ARGUMENT ->
                                    Uni.createFrom().item(Targeted.of(errorExchangeName, new IncrementStockErrorMessage(t, IncrementStockErrorMessage.Type.INVALID_QUANTITY, incrementStockMessage.itemId())));
                            default -> {
                                LOGGER.error("Unexpected error during stock increment", t);
                                yield Uni.createFrom().item(Targeted.of(errorExchangeName, new IncrementStockErrorMessage(t, IncrementStockErrorMessage.Type.UNEXPECTED_ERROR, incrementStockMessage.itemId())));
                            }
                        };
                    } else {
                        LOGGER.error("Unexpected error during stock increment", t);
                        return Uni.createFrom().item(Targeted.of(errorExchangeName, new IncrementStockErrorMessage(t, IncrementStockErrorMessage.Type.UNEXPECTED_ERROR, incrementStockMessage.itemId())));
                    }
                });
    }
}
