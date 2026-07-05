package org.stevidigital.productcatalog.infrastructure.metrics;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.MeterBinder;
import org.springframework.stereotype.Component;
import org.stevidigital.productcatalog.domain.model.ProductStatus;
import org.stevidigital.productcatalog.domain.repository.ProductRepository;

/**
 * METER BINDER — registers a Gauge per ProductStatus.
 *
 * Prometheus scrapes this on each collection cycle, calling productRepository.findAll()
 * to compute the current count per status. In-memory this is cheap; in Episode 2
 * (PostgreSQL) this becomes a COUNT(*) GROUP BY status query, which should be indexed.
 *
 * PromQL to visualize the gauge:
 *   product_catalog_products_total{status="DRAFT"}
 *   product_catalog_products_total{status="PUBLISHED"}
 *   product_catalog_products_total{status="DISCONTINUED"}
 */
@Component
public class ProductStatusGaugeBinder implements MeterBinder {

    private final ProductRepository productRepository;

    public ProductStatusGaugeBinder(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    @Override
    public void bindTo(MeterRegistry registry) {
        for (ProductStatus status : ProductStatus.values()) {
            Gauge.builder("product_catalog_products_total",
                            productRepository,
                            repo -> repo.findAll().stream()
                                    .filter(p -> p.status() == status)
                                    .count())
                    .tag("status", status.name())
                    .description("Total products in the catalog by lifecycle status")
                    .register(registry);
        }
    }
}
