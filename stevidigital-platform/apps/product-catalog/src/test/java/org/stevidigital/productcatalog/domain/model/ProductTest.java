package org.stevidigital.productcatalog.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.stevidigital.productcatalog.domain.event.ProductCreated;
import org.stevidigital.productcatalog.domain.event.ProductDiscontinued;
import org.stevidigital.productcatalog.domain.event.ProductPublished;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for the Product aggregate.
 *
 * No Spring, no DB, no network — pure Java. These tests run in milliseconds
 * and document the EXACT business rules the aggregate enforces.
 *
 * [Evans] Ch.6: Tests for an AGGREGATE should read like a specification of
 * the domain rules, not like a list of implementation details.
 *
 * [Majors] Ch.5: "Tests are the first form of observability." A failing test
 * at this level pinpoints the exact invariant that was violated, not just
 * "the endpoint returned 422."
 */
class ProductTest {

    private static final ProductId  PRODUCT_ID = ProductId.generate();
    private static final ProductName NAME       = new ProductName("Wireless Keyboard");
    private static final Category   CATEGORY   = Category.ELECTRONICS;

    @Nested
    @DisplayName("Product.create factory")
    class Creation {

        @Test
        @DisplayName("should start in DRAFT status")
        void createsInDraft() {
            var product = Product.create(PRODUCT_ID, NAME, CATEGORY);
            assertEquals(ProductStatus.DRAFT, product.status());
        }

        @Test
        @DisplayName("should register a ProductCreated event on creation")
        void registersCreatedEvent() {
            var product = Product.create(PRODUCT_ID, NAME, CATEGORY);
            var events  = product.pullDomainEvents();

            assertEquals(1, events.size());
            assertInstanceOf(ProductCreated.class, events.getFirst());

            var event = (ProductCreated) events.getFirst();
            assertEquals(PRODUCT_ID, event.productId());
            assertEquals(NAME,       event.name());
            assertEquals(CATEGORY,   event.category());
        }

        @Test
        @DisplayName("pullDomainEvents drains the buffer — idempotent drain")
        void pullDrainsBuffer() {
            var product = Product.create(PRODUCT_ID, NAME, CATEGORY);
            product.pullDomainEvents(); // drain once
            assertTrue(product.pullDomainEvents().isEmpty(), "Second pull must be empty");
        }
    }

    @Nested
    @DisplayName("Product.publish lifecycle")
    class Publishing {

        @Test
        @DisplayName("DRAFT → PUBLISHED should succeed and emit ProductPublished")
        void publishesDraft() {
            var product = Product.create(PRODUCT_ID, NAME, CATEGORY);
            product.pullDomainEvents(); // drain creation events

            product.publish();

            assertEquals(ProductStatus.PUBLISHED, product.status());
            var events = product.pullDomainEvents();
            assertEquals(1, events.size());
            assertInstanceOf(ProductPublished.class, events.getFirst());
        }

        @Test
        @DisplayName("PUBLISHED → PUBLISHED should throw (not idempotent)")
        void publishingPublishedThrows() {
            var product = Product.create(PRODUCT_ID, NAME, CATEGORY);
            product.publish();

            assertThrows(IllegalStateException.class, product::publish);
        }

        @Test
        @DisplayName("DISCONTINUED → PUBLISHED should throw")
        void publishingDiscontinuedThrows() {
            var product = Product.create(PRODUCT_ID, NAME, CATEGORY);
            product.publish();
            product.discontinue();

            assertThrows(IllegalStateException.class, product::publish);
        }
    }

    @Nested
    @DisplayName("Product.discontinue lifecycle")
    class Discontinuation {

        @Test
        @DisplayName("PUBLISHED → DISCONTINUED should succeed and emit ProductDiscontinued")
        void discontinuesPublished() {
            var product = Product.create(PRODUCT_ID, NAME, CATEGORY);
            product.publish();
            product.pullDomainEvents(); // drain creation + published

            product.discontinue();

            assertEquals(ProductStatus.DISCONTINUED, product.status());
            var events = product.pullDomainEvents();
            assertEquals(1, events.size());
            assertInstanceOf(ProductDiscontinued.class, events.getFirst());
        }

        @Test
        @DisplayName("DRAFT → DISCONTINUED should throw")
        void discontinuingDraftThrows() {
            var product = Product.create(PRODUCT_ID, NAME, CATEGORY);
            assertThrows(IllegalStateException.class, product::discontinue);
        }
    }
}
