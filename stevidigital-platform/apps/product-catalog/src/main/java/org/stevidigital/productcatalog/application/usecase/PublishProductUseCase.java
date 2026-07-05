package org.stevidigital.productcatalog.application.usecase;

import org.stevidigital.productcatalog.application.command.PublishProductCommand;
import org.stevidigital.productcatalog.domain.model.ProductId;
import org.stevidigital.productcatalog.domain.repository.ProductRepository;

/**
 * APPLICATION SERVICE — transitions a product from DRAFT to PUBLISHED.
 *
 * Lifecycle enforcement lives in Product.publish() — not here.
 * This class only orchestrates: load → mutate → save → dispatch.
 *
 * [Evans] Ch.4: "The application layer should be thin. All meaningful work
 * should be delegated to the domain layer."
 */
public class PublishProductUseCase {

    private final ProductRepository repository;
    private final DomainEventPublisher eventPublisher;
    private final ProductCatalogMetrics metrics;

    public PublishProductUseCase(ProductRepository repository,
                                 DomainEventPublisher eventPublisher,
                                 ProductCatalogMetrics metrics) {
        this.repository = repository;
        this.eventPublisher = eventPublisher;
        this.metrics = metrics;
    }

    public void execute(PublishProductCommand command) {
        var id      = ProductId.of(command.productId());
        var product = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Product not found: " + id));

        product.publish();

        repository.save(product);

        product.pullDomainEvents().forEach(eventPublisher::publish);

        metrics.recordProductPublished();
    }
}
