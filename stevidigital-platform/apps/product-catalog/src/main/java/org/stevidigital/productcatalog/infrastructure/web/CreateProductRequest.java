package org.stevidigital.productcatalog.infrastructure.web;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Web DTO — JSON request body for POST /api/v1/products.
 *
 * Deliberately separate from CreateProductCommand: this owns the HTTP contract
 * (validation annotations, OpenAPI schema); the command owns the application contract.
 */
@Schema(description = "Request body to create a new product")
public record CreateProductRequest(

        @Schema(description = "Product display name", example = "Wireless Keyboard",
                minLength = 1, maxLength = 200)
        @NotBlank(message = "name must not be blank")
        @Size(max = 200, message = "name must not exceed 200 characters")
        String name,

        @Schema(description = "Product category",
                example = "ELECTRONICS",
                allowableValues = {"ELECTRONICS", "CLOTHING", "BOOKS", "HOME", "SPORTS", "FOOD", "OTHER"})
        @NotBlank(message = "category must not be blank")
        String category

) {}
