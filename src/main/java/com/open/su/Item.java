package com.open.su;

import io.quarkus.hibernate.reactive.panache.PanacheEntityBase;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.util.Date;
import java.util.Optional;
import java.util.UUID;

@Entity
public class Item extends PanacheEntityBase {

    @Id
    @Column(nullable = false, unique = true)
    UUID id;
    @Column(nullable = false)
    Long stock;
    @Column(name = "max_stock", nullable = false)
    Long maxStock;
    @Column(name = "min_stock", nullable = false)
    Long minStock = 0L;
    @Column(name = "created_at")
    @CreationTimestamp
    Date createdAt;
    @Column(name = "updated_at")
    @UpdateTimestamp
    Date updatedAt;

    /**
     * Create a new item from a {@link CreateItemRequest}
     *
     * @param request the grpc request
     */
    public Item(CreateItemRequest request) {
        this.id = UUID.fromString(request.getId());
        this.stock = request.getMaxStock();
        this.maxStock = request.getMaxStock();
        this.minStock = request.hasMinStock() ? request.getMinStock() : 0L;
    }

    /**
     * Create a new item from a {@link UpdateItemRequest}
     *
     * @param request the grpc request
     */
    public Item(UpdateItemRequest request) {
        this.id = UUID.fromString(request.getId());
        this.stock = request.hasMinStock() ? request.getMinStock() : null;
        this.maxStock = request.hasMaxStock() ? request.getMaxStock() : null;
    }

    public Item() {

    }

    public Item update(Item item) {
        this.stock = Optional.ofNullable(item.stock).orElse(this.stock);
        this.maxStock = Optional.ofNullable(item.maxStock).orElse(this.maxStock);
        this.minStock = Optional.ofNullable(item.minStock).orElse(this.minStock);
        return this;
    }

    /**
     * Convert the item to a {@link ListItemsResponse}
     *
     * @return the grpc response
     */
    public ListItemsResponse toListItemsResponse() {
        return ListItemsResponse.newBuilder()
                .setId(this.id.toString())
                .setStock(this.stock)
                .setMaxStock(this.maxStock)
                .setMinStock(this.minStock).build();
    }

    /**
     * Convert the item to a {@link GetItemDetailsResponse}
     *
     * @return the grpc response
     */
    public GetItemDetailsResponse toGetItemDetailsResponse() {
        return GetItemDetailsResponse.newBuilder()
                .setId(this.id.toString())
                .setStock(this.stock)
                .setMaxStock(this.maxStock)
                .setMinStock(this.minStock)
                .setCreatedAt(this.createdAt.toInstant().toString())
                .setUpdatedAt(this.updatedAt.toInstant().toString())
                .build();
    }
}
