package org.stevidigital.productcatalog.application.usecase;

/**
 * PORT — metrics boundary for the application layer.
 *
 * Keeps use cases free of Micrometer/infrastructure imports.
 * Implemented by MicrometerProductCatalogMetrics in the infrastructure layer.
 */
public interface ProductCatalogMetrics {

    void recordProductCreated();

    void recordProductPublished();

    void recordDomainEventPublished(String eventType);
}
