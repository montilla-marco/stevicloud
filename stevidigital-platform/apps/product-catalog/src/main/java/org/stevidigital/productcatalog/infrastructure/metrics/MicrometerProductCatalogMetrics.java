package org.stevidigital.productcatalog.infrastructure.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;
import org.stevidigital.productcatalog.application.usecase.ProductCatalogMetrics;

/**
 * ADAPTER — Micrometer implementation of the ProductCatalogMetrics port.
 *
 * Counters use the same metric name with different tag values so Prometheus
 * can aggregate or filter: sum(product_catalog_operations_total) vs
 * product_catalog_operations_total{operation="create"}.
 *
 * Domain event counter uses a dynamic tag (event_type) so each event type
 * appears as a separate series without requiring a separate Counter field per type.
 */
@Component
public class MicrometerProductCatalogMetrics implements ProductCatalogMetrics {

    private static final String OPERATIONS_METRIC = "product_catalog_operations_total";
    private static final String EVENTS_METRIC     = "product_catalog_domain_events_total";

    private final Counter productCreatedCounter;
    private final Counter productPublishedCounter;
    private final MeterRegistry registry;

    public MicrometerProductCatalogMetrics(MeterRegistry registry) {
        this.registry = registry;

        this.productCreatedCounter = Counter.builder(OPERATIONS_METRIC)
                .tag("operation", "create")
                .description("Total product create operations")
                .register(registry);

        this.productPublishedCounter = Counter.builder(OPERATIONS_METRIC)
                .tag("operation", "publish")
                .description("Total product publish operations")
                .register(registry);
    }

    @Override
    public void recordProductCreated() {
        productCreatedCounter.increment();
    }

    @Override
    public void recordProductPublished() {
        productPublishedCounter.increment();
    }

    @Override
    public void recordDomainEventPublished(String eventType) {
        // Dynamic tag — Micrometer creates one Counter per distinct eventType value.
        Counter.builder(EVENTS_METRIC)
                .tag("event_type", eventType)
                .description("Total domain events published by type")
                .register(registry)
                .increment();
    }
}
