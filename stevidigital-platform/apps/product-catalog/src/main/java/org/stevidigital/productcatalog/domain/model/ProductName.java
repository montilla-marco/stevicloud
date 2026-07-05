package org.stevidigital.productcatalog.domain.model;

import java.util.Objects;

/**
 * Value Object — enforces invariants at the type level.
 *
 * [Evans] Ch.5 p.99: "VALUE OBJECTS can reference ENTITIES [...] but the key
 * distinction is that a VALUE OBJECT has no conceptual identity."
 *
 * Why not just String?
 * A String accepts "", " ", null, and "x".repeat(10_000).
 * ProductName encodes the rules of what a valid product name IS,
 * making those rules impossible to bypass from outside the domain.
 * This is the difference between a model that expresses business rules
 * and a bag of primitives. [Evans] Ch.9 — Making Implicit Concepts Explicit.
 */
public record ProductName(String value) {

    private static final int MAX_LENGTH = 200;

    public ProductName {
        Objects.requireNonNull(value, "ProductName cannot be null");
        String trimmed = value.strip();
        if (trimmed.isBlank())
            throw new IllegalArgumentException("ProductName cannot be blank");
        if (trimmed.length() > MAX_LENGTH)
            throw new IllegalArgumentException(
                "ProductName cannot exceed %d characters, got %d".formatted(MAX_LENGTH, trimmed.length()));
        value = trimmed;
    }

    @Override
    public String toString() {
        return value;
    }
}
