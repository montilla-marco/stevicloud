package org.stevidigital.productcatalog.application.command;

import org.stevidigital.productcatalog.domain.model.Category;

/**
 * Application-layer DTO — carries the intent to create a product.
 *
 * [Evans] Ch.4: "The application layer is responsible for [...] directing the domain
 * objects to perform the actual work."
 *
 * Commands are plain data — no business logic, no domain objects.
 * The use case converts them into domain objects (ProductId, ProductName, Category)
 * and calls the aggregate. This separation means:
 * - The REST controller doesn't import domain types directly
 * - The command can be deserialized from HTTP or from a Kafka message (same object)
 *
 * Named in imperative mood (CreateProduct) — it's an intent, not a fact.
 * Compare: ProductCreated (event, past tense) vs CreateProductCommand (intent).
 */
public record CreateProductCommand(String name, String category) {}
