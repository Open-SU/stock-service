package com.open.su;

import com.open.su.exceptions.ItemServiceException;
import io.quarkus.hibernate.reactive.panache.common.WithTransaction;
import io.quarkus.panache.common.Page;
import io.quarkus.panache.common.Sort;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.tuples.Tuple2;
import jakarta.enterprise.context.ApplicationScoped;
import org.jboss.logging.Logger;

import java.util.List;
import java.util.UUID;

@ApplicationScoped
@WithTransaction
public class ItemService {

    private static final Logger LOGGER = Logger.getLogger(ItemService.class);

    /**
     * List items with pagination and sorting
     *
     * @param page page number and size
     * @param sort sort by field and direction
     * @return a {@link Uni} with the list of items (with minimal information)
     */
    public Uni<List<Item>> listItems(Page page, Sort sort) {
        LOGGER.trace("Listing items with page " + page + " and sort " + sort);
        return Item.<Item>findAll(sort).page(page).list()
                .onFailure().transform(t -> {
                    String message = "Failed to list items";
                    LOGGER.error("[" + Method.LIST + "] " + message, t);
                    return ItemServiceException.DATABASE_ERROR.withCause(t).withMessage(message);
                });
    }

    /**
     * Get item details
     *
     * @param id the id of the item
     * @return a {@link Uni} with the item details
     */
    public Uni<Item> getItemDetails(UUID id) {
        LOGGER.trace("Getting item details for item with id " + id);
        return findItemOrFail(id, Method.DETAILS);
    }

    /**
     * Create an item
     *
     * @param item the item to create
     * @return a {@link Uni} with the id of the created item
     */
    public Uni<UUID> createItem(Item item) {
        LOGGER.trace("Creating item " + item);
        return checkItemProperties(item)
                .onItem().transformToUni(v -> Item.<Item>findById(item.id)
                        .onFailure().transform(t -> {
                            String message = "Failed to get item with id " + item.id;
                            LOGGER.error("[" + Method.CREATE + "] " + message, t);
                            return ItemServiceException.DATABASE_ERROR.withCause(t).withMessage(message);
                        })
                        .onItem().ifNotNull().failWith(() -> {
                            String message = "Item with id " + item.id + " already exists";
                            LOGGER.debug("[" + Method.CREATE + "] " + message);
                            return ItemServiceException.CONFLICT.withMessage(message);
                        })
                        .onItem().transformToUni(e -> persistItemOrFail(item, Method.CREATE))
                        .onItem().transform(e -> e == null ? null : e.id));
    }

    /**
     * Update an item
     *
     * @param item the item to update
     * @return a {@link Uni} with the id of the updated item
     */
    public Uni<UUID> updateItem(Item item) {
        LOGGER.trace("Updating item " + item);
        return checkItemProperties(item)
                .onItem().transformToUni(v -> findItemOrFail(item.id, Method.UPDATE)
                        .onItem().transformToUni(existingItem -> {
                            if (item.minStock != null && item.minStock > existingItem.maxStock) {
                                return Uni.createFrom().failure(ItemServiceException.INVALID_ARGUMENT.withMessage("Minimum stock must be less than maximum stock"));
                            }
                            if (item.maxStock != null && item.maxStock < existingItem.minStock) {
                                return Uni.createFrom().failure(ItemServiceException.INVALID_ARGUMENT.withMessage("Maximum stock must be greater than minimum stock"));
                            }
                            return persistItemOrFail(existingItem.update(item), Method.UPDATE);
                        })
                        .onItem().transform(e -> e == null ? null : e.id));
    }

    /**
     * Increment an item stock
     *
     * @param id        the id of the item to update
     * @param increment the increment to apply to the stock (can be negative)
     * @return a {@link Uni} with the id of the updated item
     */
    public Uni<Tuple2<UUID, Long>> incrementItemStock(UUID id, Long increment) {
        LOGGER.trace("Incrementing stock by " + increment + " for item with id " + id);
        return findItemOrFail(id, Method.UPDATE)
                .onItem().transformToUni(existingItem -> {
                    if (existingItem.stock + increment < existingItem.minStock) {
                        return Uni.createFrom().failure(ItemServiceException.INVALID_ARGUMENT.withMessage("Stock cannot be less than minimum stock"));
                    }
                    if (existingItem.stock + increment > existingItem.maxStock) {
                        return Uni.createFrom().failure(ItemServiceException.INVALID_ARGUMENT.withMessage("Stock cannot be greater than maximum stock"));
                    }
                    existingItem.stock += increment;
                    return persistItemOrFail(existingItem, Method.UPDATE);
                })
                .onItem().transform(e -> e == null ? null : Tuple2.of(e.id, e.stock));
    }

    /**
     * Delete an item
     *
     * @param id the id of the item to delete
     * @return a {@link Uni} of Void
     */
    public Uni<Void> deleteItem(UUID id) {
        LOGGER.trace("Deleting item with id " + id);
        return findItemOrFail(id, Method.DELETE)
                .onItem().transformToUni(existingItem ->
                        existingItem.delete()
                                .onFailure().transform(t -> {
                                    String message = "Failed to delete item with id " + id;
                                    LOGGER.error("[" + Method.DELETE + "] " + message, t);
                                    return ItemServiceException.DATABASE_ERROR.withCause(t).withMessage(message);
                                })
                                .onItem().invoke(() -> LOGGER.debug("[" + Method.DELETE + "] " + "Deleted item with id " + id)));
    }

    /**
     * Find an item by id or fail
     *
     * @param id     the id of the item
     * @param method the context in which the find is performed (for logging purposes)
     * @return a {@link Uni} with the item, otherwise a failed {@link Uni}
     */
    Uni<Item> findItemOrFail(UUID id, Method method) {
        return Item.<Item>findById(id)
                .onFailure().transform(t -> {
                    String message = "Failed to get item with id " + id;
                    LOGGER.error("[" + method + "] " + message, t);
                    return ItemServiceException.DATABASE_ERROR.withCause(t).withMessage(message);
                })
                .onItem().ifNull().failWith(() -> {
                    String message = "Item with id " + id + " does not exist";
                    LOGGER.debug("[" + method + "] " + message);
                    return ItemServiceException.NOT_FOUND.withMessage(message);
                });
    }

    /**
     * Persist an item or fail
     *
     * @param item   the item to persist
     * @param method the context in which the persist is performed (for logging purposes)
     * @return a {@link Uni} with the persisted item, otherwise a failed {@link Uni}
     */
    Uni<Item> persistItemOrFail(Item item, Method method) {
        return item.<Item>persist()
                .onFailure().transform(t -> {
                    String message = "Failed to persist stock for item with id " + item.id;
                    LOGGER.error("[" + method + "] " + message, t);
                    return ItemServiceException.DATABASE_ERROR.withCause(t).withMessage(message);
                })
                .onItem().ifNotNull().invoke(existingItem -> LOGGER.debug("[" + method + "] Persisted item with id " + existingItem.id));
    }

    Uni<Void> checkItemProperties(Item item) {
        if (item.minStock != null && item.minStock < 0) {
            return Uni.createFrom().failure(ItemServiceException.INVALID_ARGUMENT.withMessage("Minimum stock must be greater than 0"));
        }
        if (item.maxStock != null && item.maxStock <= 0) {
            return Uni.createFrom().failure(ItemServiceException.INVALID_ARGUMENT.withMessage("Maximum stock must be greater than 0 and greater than minimum stock"));
        }
        if (item.minStock != null && item.maxStock != null && item.minStock > item.maxStock) {
            return Uni.createFrom().failure(ItemServiceException.INVALID_ARGUMENT.withMessage("Minimum stock must be less than maximum stock"));
        }
        return Uni.createFrom().voidItem();
    }

    /**
     * Lis of methods for logging purposes
     */
    enum Method {
        LIST,
        DETAILS,
        CREATE,
        UPDATE,
        DELETE,
    }
}
