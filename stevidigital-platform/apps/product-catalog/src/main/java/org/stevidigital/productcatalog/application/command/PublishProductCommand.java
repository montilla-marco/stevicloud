package org.stevidigital.productcatalog.application.command;

/**
 * Application-layer DTO — intent to publish an existing DRAFT product.
 *
 * The productId comes from the caller (URL path param in REST, message key in Kafka).
 * The use case loads the aggregate, calls product.publish(), and saves it back.
 */
public record PublishProductCommand(String productId) {}
