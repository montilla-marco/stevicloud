package org.stevidigital.productcatalog.infrastructure.web;

import io.swagger.v3.oas.annotations.media.Schema;
import org.stevidigital.productcatalog.domain.model.Product;

/**
 * Web DTO — JSON response for product endpoints. Translates domain objects to
 * plain strings at the HTTP boundary — the ACL in miniature. [Evans] Ch.14.
 */
@Schema(description = "Product representation")
public record ProductResponse(

        @Schema(description = "Product UUID", example = "550e8400-e29b-41d4-a716-446655440000")
        String id,

        @Schema(description = "Product name", example = "Wireless Keyboard")
        String name,

        @Schema(description = "Product category", example = "ELECTRONICS")
        String category,

        @Schema(description = "Lifecycle status", example = "DRAFT",
                allowableValues = {"DRAFT", "PUBLISHED", "DISCONTINUED"})
        String status

) {
    public static ProductResponse from(Product product) {
        return new ProductResponse(
                product.id().value(),
                product.name().value(),
                product.category().name(),
                product.status().name()
        );
    }
}
