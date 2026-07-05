package org.stevidigital.productcatalog.domain.model;

import java.util.Objects;
import java.util.UUID;

/**
 * Value Object — identity of a Product within the ProductCatalog bounded context.
 *
 * [Evans] Ch.5 p.97: "When you care only about the attributes of an element of the model,
 * classify it as a VALUE OBJECT. Make it immutable."
 *
 * Note: ProductId in Pricing BC or Inventory BC will be a different type with the same
 * string value — each BC owns its own representation of identity.
 * [Evans] Ch.14 — Context Map, Shared Kernel.
 */
public record ProductId(String value) {

    public ProductId {
        Objects.requireNonNull(value, "ProductId cannot be null");
        if (value.isBlank()) throw new IllegalArgumentException("ProductId cannot be blank");
    }

    public static ProductId generate() {
        return new ProductId(UUID.randomUUID().toString());
    }

    public static ProductId of(String value) {
        return new ProductId(value);
    }

    @Override
    public String toString() {
        return value;
    }
}
