package org.stevidigital.productcatalog.application.usecase;

import org.stevidigital.productcatalog.application.command.CreateProductCommand;
import org.stevidigital.productcatalog.domain.event.DomainEvent;
import org.stevidigital.productcatalog.domain.model.Category;
import org.stevidigital.productcatalog.domain.model.Product;
import org.stevidigital.productcatalog.domain.model.ProductId;
import org.stevidigital.productcatalog.domain.model.ProductName;
import org.stevidigital.productcatalog.domain.repository.ProductRepository;

import java.util.List;

/**
 * APPLICATION SERVICE — orchestrates one use case: create a product.
 *
 * [Evans] Ch.4: "APPLICATION LAYER: Defines the jobs the software is supposed to do
 * and directs the expressive domain objects to work out problems. [...] It does not
 * contain business rules or knowledge."
 *
 * This class has three responsibilities:
 * 1. Translate the command (raw strings) into domain objects
 * 2. Call the aggregate factory (Product.create)
 * 3. Persist and dispatch domain events
 *
 * What it does NOT do:
 * - Enforce "only draft products can be published" — that's Product's job
 * - Know how events are dispatched (Kafka, in-memory, outbox) — that's infrastructure
 *
 * [Ford-FSA] Ch.8: The application layer is the only layer that imports both domain
 * and infrastructure. It is the seam where the hexagon closes.
 */
public class CreateProductUseCase {

    private final ProductRepository repository;
    private final DomainEventPublisher eventPublisher;
    private final ProductCatalogMetrics metrics;

    public CreateProductUseCase(ProductRepository repository,
                                DomainEventPublisher eventPublisher,
                                ProductCatalogMetrics metrics) {
        this.repository = repository;
        this.eventPublisher = eventPublisher;
        this.metrics = metrics;
    }

    public ProductId execute(CreateProductCommand command) {
        var id       = ProductId.generate();
        var name     = new ProductName(command.name());
        var category = Category.valueOf(command.category().toUpperCase());

        var product = Product.create(id, name, category);

        repository.save(product);

        // Drain and publish AFTER save — events should only fire for persisted state.
        // In Episode 2 we'll replace this with a transactional outbox + Kafka.
        List<DomainEvent> events = product.pullDomainEvents();
        events.forEach(eventPublisher::publish);

        metrics.recordProductCreated();

        return id;
    }
}
