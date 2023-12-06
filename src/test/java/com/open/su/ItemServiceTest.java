package com.open.su;

import com.open.su.exceptions.ItemServiceException;
import io.quarkus.hibernate.reactive.panache.Panache;
import io.quarkus.panache.common.Page;
import io.quarkus.panache.common.Sort;
import io.quarkus.test.hibernate.reactive.panache.TransactionalUniAsserter;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.vertx.RunOnVertxContext;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

@QuarkusTest
class ItemServiceTest {
    @Inject
    ItemService itemService;

    @RunOnVertxContext
    @Test
    void testListItems(TransactionalUniAsserter asserter) {
        asserter.execute(() -> {
            Item item1 = new Item();
            item1.id = UUID.randomUUID();
            item1.maxStock = 10L;
            item1.minStock = 5L;
            item1.stock = 7L;
            Item item2 = new Item();
            item2.id = UUID.randomUUID();
            item2.maxStock = 10L;
            item2.minStock = 5L;
            item2.stock = 8L;
            Item item3 = new Item();
            item3.id = UUID.randomUUID();
            item3.maxStock = 10L;
            item3.minStock = 5L;
            item3.stock = 9L;

            asserter.putData("items", List.of(item1, item2, item3));

            return item1.persist().chain(item2::persist).chain(item3::persist);
        });

        asserter.assertThat(() -> {
            Page page = Page.of(0, 10);
            Sort sort = Sort.by("stock", Sort.Direction.Ascending);

            return itemService.listItems(page, sort);
        }, response -> {
            List<Item> items = (List<Item>) asserter.getData("items");
            Assertions.assertEquals(items.get(0), response.get(0));
            Assertions.assertEquals(items.get(1), response.get(1));
            Assertions.assertEquals(items.get(2), response.get(2));
        });

        asserter.execute(() -> Item.deleteAll());

        asserter.surroundWith(u -> Panache.withSession(() -> u));
    }

    @RunOnVertxContext
    @Test
    void testGetItemDetails(TransactionalUniAsserter asserter) {
        asserter.execute(() -> {
            Item item = new Item();
            item.id = UUID.randomUUID();
            item.maxStock = 10L;
            item.minStock = 5L;
            item.stock = 7L;

            asserter.putData("item", item);

            return item.persist();
        });

        asserter.assertThat(() -> {
            Item item = (Item) asserter.getData("item");

            return itemService.getItemDetails(item.id);
        }, response -> {
            Item item = (Item) asserter.getData("item");
            Assertions.assertEquals(item, response);
        });

        asserter.assertFailedWith(() -> itemService.getItemDetails(UUID.randomUUID())
                , e -> Assertions.assertSame(ItemServiceException.Type.NOT_FOUND, ((ItemServiceException) e).getType()));

        asserter.execute(() -> Item.deleteAll());

        asserter.surroundWith(u -> Panache.withSession(() -> u));
    }

    @RunOnVertxContext
    @Test
    void testCreateItem(TransactionalUniAsserter asserter) {
        asserter.execute(() -> {
            Item item1 = new Item();
            item1.id = UUID.randomUUID();
            item1.maxStock = 10L;
            item1.minStock = 5L;
            item1.stock = 7L;
            Item item2 = new Item();
            item2.id = UUID.randomUUID();
            item2.maxStock = 10L;
            item2.minStock = 0L;
            item2.stock = 8L;

            asserter.putData("item1", item1);
            asserter.putData("item2", item2);

            return item1.persist();
        });

        asserter.assertThat(() -> {
            Item item = (Item) asserter.getData("item2");

            return itemService.createItem(item);
        }, response -> {
            Item item = (Item) asserter.getData("item2");
            Assertions.assertEquals(item.id, response);
        });

        asserter.assertThat(() -> {
            Item item = (Item) asserter.getData("item2");

            return Item.findById(item.id);
        }, response -> {
            Item item = (Item) asserter.getData("item2");
            Assertions.assertEquals(item, response);
        });

        asserter.assertFailedWith(() -> {
            Item item = new Item();
            item.id = UUID.randomUUID();
            item.maxStock = 0L;
            item.minStock = 5L;
            item.stock = 7L;

            return itemService.createItem(item);
        }, e -> Assertions.assertSame(ItemServiceException.Type.INVALID_ARGUMENT, ((ItemServiceException) e).getType()));

        asserter.assertFailedWith(() -> {
            Item item = new Item();
            item.id = UUID.randomUUID();
            item.maxStock = 1L;
            item.minStock = -5L;
            item.stock = 7L;

            return itemService.createItem(item);
        }, e -> Assertions.assertSame(ItemServiceException.Type.INVALID_ARGUMENT, ((ItemServiceException) e).getType()));

        asserter.assertFailedWith(() -> {
            Item item = new Item();
            item.id = ((Item) asserter.getData("item1")).id;
            item.maxStock = 10L;
            item.minStock = 5L;
            item.stock = 7L;

            return itemService.createItem(item);
        }, e -> Assertions.assertSame(ItemServiceException.Type.CONFLICT, ((ItemServiceException) e).getType()));

        asserter.execute(() -> Item.deleteAll());

        asserter.surroundWith(u -> Panache.withSession(() -> u));
    }

    @RunOnVertxContext
    @Test
    void testUpdateItem(TransactionalUniAsserter asserter) {
        asserter.execute(() -> {
            Item item1 = new Item();
            item1.id = UUID.randomUUID();
            item1.maxStock = 10L;
            item1.minStock = 5L;
            item1.stock = 7L;
            Item item2 = new Item();
            item2.id = UUID.randomUUID();
            item2.maxStock = 10L;
            item2.minStock = 5L;
            item2.stock = 8L;

            asserter.putData("item1", item1);
            asserter.putData("item2", item2);

            return item1.persist();
        });

        asserter.assertThat(() -> {
            Item item1 = (Item) asserter.getData("item1");
            Item item2 = (Item) asserter.getData("item2");
            item1.minStock = item2.minStock;

            return itemService.updateItem(item1);
        }, response -> {
            Item item = (Item) asserter.getData("item1");
            Assertions.assertEquals(item.id, response);
        });

        asserter.assertThat(() -> {
            Item item = (Item) asserter.getData("item1");

            return Item.findById(item.id);
        }, response -> {
            Item item = (Item) asserter.getData("item2");
            Assertions.assertEquals(item.minStock, ((Item) response).minStock);
        });

        asserter.assertFailedWith(() -> {
            Item item = new Item();
            item.id = UUID.randomUUID();
            item.id = UUID.randomUUID();
            item.maxStock = -1L;
            item.minStock = 5L;
            item.stock = 7L;

            return itemService.updateItem(item);
        }, e -> Assertions.assertSame(ItemServiceException.Type.INVALID_ARGUMENT, ((ItemServiceException) e).getType()));

        asserter.assertFailedWith(() -> {
            Item item = new Item();
            item.id = UUID.randomUUID();
            item.id = UUID.randomUUID();
            item.maxStock = 1L;
            item.minStock = -5L;
            item.stock = 7L;

            return itemService.updateItem(item);
        }, e -> Assertions.assertSame(ItemServiceException.Type.INVALID_ARGUMENT, ((ItemServiceException) e).getType()));

        asserter.assertFailedWith(() -> {
            Item item = new Item();
            item.id = UUID.randomUUID();
            item.id = UUID.randomUUID();
            item.maxStock = 10L;
            item.minStock = 5L;
            item.stock = 7L;

            return itemService.updateItem(item);
        }, e -> Assertions.assertSame(ItemServiceException.Type.NOT_FOUND, ((ItemServiceException) e).getType()));

        asserter.execute(() -> Item.deleteAll());

        asserter.surroundWith(u -> Panache.withSession(() -> u));
    }

    @RunOnVertxContext
    @Test
    void testIncrementItemStock(TransactionalUniAsserter asserter) {
        asserter.execute(() -> {
            Item item1 = new Item();
            item1.id = UUID.randomUUID();
            item1.maxStock = 10L;
            item1.minStock = 5L;
            item1.stock = 7L;
            Item item2 = new Item();
            item2.id = UUID.randomUUID();
            item2.maxStock = 10L;
            item2.minStock = 5L;
            item2.stock = 8L;

            asserter.putData("item1", item1);
            asserter.putData("item2", item2);

            return item1.persist();
        });

        asserter.assertThat(() -> {
            Item item = (Item) asserter.getData("item1");

            return itemService.incrementItemStock(item.id, 1L);
        }, response -> {
            Item item1 = (Item) asserter.getData("item1");
            Item item2 = (Item) asserter.getData("item2");
            Assertions.assertEquals(item1.id, response.getItem1());
            Assertions.assertEquals(item2.stock, response.getItem2());
        });

        asserter.assertThat(() -> {
            Item item = (Item) asserter.getData("item1");

            return Item.findById(item.id);
        }, response -> {
            Item item = (Item) asserter.getData("item2");
            Assertions.assertEquals(item.stock, ((Item) response).stock);
        });

        asserter.assertFailedWith(() -> itemService.incrementItemStock(UUID.randomUUID(), 1L)
                , e -> Assertions.assertSame(ItemServiceException.Type.NOT_FOUND, ((ItemServiceException) e).getType()));

        asserter.execute(() -> Item.deleteAll());

        asserter.surroundWith(u -> Panache.withSession(() -> u));
    }

    @RunOnVertxContext
    @Test
    void testDeleteItem(TransactionalUniAsserter asserter) {
        asserter.execute(() -> {
            Item item = new Item();
            item.id = UUID.randomUUID();
            item.maxStock = 10L;
            item.minStock = 5L;
            item.stock = 7L;

            asserter.putData("item", item);

            return item.persist();
        });

        asserter.assertFailedWith(() -> itemService.deleteItem(UUID.randomUUID())
                , e -> Assertions.assertSame(ItemServiceException.Type.NOT_FOUND, ((ItemServiceException) e).getType()));

        // Assert that calling delete does not fail (delete returns void if not failing)
        asserter.assertThat(() -> {
            Item item = (Item) asserter.getData("item");

            return itemService.deleteItem(item.id);
        }, Assertions::assertNull);

        asserter.assertFailedWith(() -> itemService.deleteItem(((Item) asserter.getData("item")).id)
                , e -> Assertions.assertSame(ItemServiceException.Type.NOT_FOUND, ((ItemServiceException) e).getType()));

        asserter.execute(() -> Item.deleteAll());

        asserter.surroundWith(u -> Panache.withSession(() -> u));
    }
}
