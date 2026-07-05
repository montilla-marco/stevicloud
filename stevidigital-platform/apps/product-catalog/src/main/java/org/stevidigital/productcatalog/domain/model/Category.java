package org.stevidigital.productcatalog.domain.model;

/**
 * Value Object modelled as an enum — finite, well-known set of valid categories
 * in the ProductCatalog Ubiquitous Language.
 *
 * [Evans] Ch.2: "Ubiquitous Language" — the names here must match exactly
 * what the domain experts use. If marketing calls it "Electronics" not "TECH",
 * this enum changes. The model reflects the language, not the other way around.
 */
public enum Category {
    ELECTRONICS,
    CLOTHING,
    BOOKS,
    HOME,
    SPORTS,
    FOOD,
    OTHER
}
