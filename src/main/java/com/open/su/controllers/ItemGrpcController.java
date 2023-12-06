package com.open.su.controllers;

import com.open.su.*;
import com.open.su.exceptions.ItemServiceException;
import io.grpc.Status;
import io.quarkus.grpc.GrpcService;
import io.quarkus.panache.common.Page;
import io.quarkus.panache.common.Sort;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

import java.util.UUID;

@GrpcService
public class ItemGrpcController implements ItemGrpc {

    private static final Logger LOGGER = Logger.getLogger(ItemGrpcController.class);

    private final ItemService itemService;

    @Inject
    public ItemGrpcController(ItemService itemService) {
        this.itemService = itemService;
    }

    /**
     * Get a paginated list of items with minimal information.
     *
     * @param request the gRPC request
     * @return the list items response
     */
    @Override
    public Multi<ListItemsResponse> listItems(ListItemsRequest request) {
        Page page = Page.of(request.hasPage() ? request.getPage() : 0, request.hasSize() ? request.getSize() : 10);
        Sort sort = Sort.by(request.hasSort() ? request.getSort() : "stock", request.hasOrder() ? Sort.Direction.valueOf(request.getOrder()) : Sort.Direction.Ascending);

        return itemService.listItems(page, sort)
                .onFailure().transform(t -> {
                    if (t instanceof ItemServiceException serviceException) {
                        return (serviceException.toGrpcException());
                    }
                    String message = "Unhandled error while listing items";
                    LOGGER.error(message, t);
                    return Status.UNKNOWN.withCause(t).withDescription(message).asRuntimeException();
                })
                .onItem().transformToMulti(items -> Multi.createFrom().iterable(items))
                .map(Item::toListItemsResponse);
    }

    /**
     * Get an item by its ID.
     *
     * @param request the gRPC request
     * @return the get item details response
     */
    @Override
    public Uni<GetItemDetailsResponse> getItemDetails(GetItemDetailsRequest request) {
        return itemService.getItemDetails(UUID.fromString(request.getId()))
                .onFailure().transform(t -> {
                    if (t instanceof ItemServiceException serviceException) {
                        return (serviceException.toGrpcException());
                    }
                    String message = "Unhandled error while getting item details";
                    LOGGER.error(message, t);
                    return Status.UNKNOWN.withCause(t).withDescription(message).asRuntimeException();
                })
                .onItem().transform(Item::toGetItemDetailsResponse);
    }

    /**
     * Create a new item.
     *
     * @param request the gRPC request
     * @return the create item response
     */
    @Override
    public Uni<CreateItemResponse> createItem(CreateItemRequest request) {
        return itemService.createItem(new Item(request))
                .onFailure().transform(t -> {
                    if (t instanceof ItemServiceException serviceException) {
                        return (serviceException.toGrpcException());
                    }
                    String message = "Unhandled error while creating item";
                    LOGGER.error(message, t);
                    return Status.UNKNOWN.withCause(t).withDescription(message).asRuntimeException();
                })
                .onItem().transform(id -> CreateItemResponse.newBuilder().setId(id.toString()).build());
    }

    /**
     * Update the stock of an existing item.
     *
     * @param request the gRPC request
     * @return the update item response
     */
    @Override
    public Uni<IncrementItemStockResponse> incrementItemStock(IncrementItemStockRequest request) {
        return itemService.incrementItemStock(UUID.fromString(request.getId()), request.getQuantity())
                .onFailure().transform(t -> {
                    if (t instanceof ItemServiceException serviceException) {
                        return (serviceException.toGrpcException());
                    }
                    String message = "Unhandled error while updating item";
                    LOGGER.error(message, t);
                    return Status.UNKNOWN.withCause(t).withDescription(message).asRuntimeException();
                })
                .onItem().transform(tuple -> IncrementItemStockResponse.newBuilder().setId(tuple.getItem1().toString()).setStock(tuple.getItem2()).build());
    }

    /**
     * Update an existing item.
     *
     * @param request the gRPC request
     * @return the update item response
     */
    @Override
    public Uni<UpdateItemResponse> updateItem(UpdateItemRequest request) {
        return itemService.updateItem(new Item(request))
                .onFailure().transform(t -> {
                    if (t instanceof ItemServiceException serviceException) {
                        return (serviceException.toGrpcException());
                    }
                    String message = "Unhandled error while updating item";
                    LOGGER.error(message, t);
                    return Status.UNKNOWN.withCause(t).withDescription(message).asRuntimeException();
                })
                .onItem().transform(id -> UpdateItemResponse.newBuilder().setId(id.toString()).build());
    }

    /**
     * Delete an existing item.
     *
     * @param request the gRPC request
     * @return the delete item response
     */
    @Override
    public Uni<DeleteItemResponse> deleteItem(DeleteItemRequest request) {
        return itemService.deleteItem(UUID.fromString(request.getId()))
                .onFailure().transform(t -> {
                    if (t instanceof ItemServiceException serviceException) {
                        return (serviceException.toGrpcException());
                    }
                    String message = "Unhandled error while deleting item";
                    LOGGER.error(message, t);
                    return Status.UNKNOWN.withCause(t).withDescription(message).asRuntimeException();
                })
                .onItem().transform(id -> DeleteItemResponse.newBuilder().build());
    }
}
