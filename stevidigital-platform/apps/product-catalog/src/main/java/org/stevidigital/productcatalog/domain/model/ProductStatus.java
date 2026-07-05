package org.stevidigital.productcatalog.domain.model;

/**
 * Value Object — lifecycle state of a Product aggregate.
 * Transitions: DRAFT → PUBLISHED → DISCONTINUED
 *
 * [Evans] Ch.6 p.126: "AGGREGATES [...] mark off the scope of change."
 * The Product aggregate enforces that only DRAFT products can be published,
 * and only PUBLISHED products can be discontinued.
 * This rule lives here, not in a service or a controller.
 */
public enum ProductStatus {
    DRAFT,
    PUBLISHED,
    DISCONTINUED
}
