package org.stevidigital.productcatalog.application.usecase;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.stevidigital.productcatalog.application.command.CreateProductCommand;
import org.stevidigital.productcatalog.domain.event.DomainEvent;
import org.stevidigital.productcatalog.domain.event.ProductCreated;
import org.stevidigital.productcatalog.domain.model.Category;
import org.stevidigital.productcatalog.domain.model.ProductStatus;
import org.stevidigital.productcatalog.infrastructure.persistence.InMemoryProductRepository;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit test for CreateProductUseCase.
 *
 * Uses InMemoryProductRepository (no DB) and a capturing DomainEventPublisher.
 * No Spring context needed — wired manually with constructor injection.
 *
 * [Evans] Ch.4: "Application services are thin coordinators."
 * These tests verify coordination, not business rules.
 * Business rules are tested in ProductTest.
 */
class CreateProductUseCaseTest {

    private InMemoryProductRepository repository;
    private CapturingEventPublisher    eventPublisher;
    private CreateProductUseCase       useCase;

    @BeforeEach
    void setUp() {
        repository     = new InMemoryProductRepository();
        eventPublisher = new CapturingEventPublisher();
        useCase        = new CreateProductUseCase(repository, eventPublisher, NoOpMetrics.INSTANCE);
    }

    @Test
    void shouldSaveProductAndPublishCreatedEvent() {
        var command = new CreateProductCommand("Wireless Keyboard", "ELECTRONICS");
        var id      = useCase.execute(command);

        var saved = repository.findById(id);
        assertTrue(saved.isPresent(), "product must be persisted");
        assertEquals(ProductStatus.DRAFT, saved.get().status());
        assertEquals("Wireless Keyboard", saved.get().name().value());
        assertEquals(Category.ELECTRONICS, saved.get().category());

        assertEquals(1, eventPublisher.captured.size());
        assertInstanceOf(ProductCreated.class, eventPublisher.captured.getFirst());
    }

    @Test
    void shouldRejectUnknownCategory() {
        var command = new CreateProductCommand("Widget", "UNKNOWN_CATEGORY");
        assertThrows(IllegalArgumentException.class, () -> useCase.execute(command));
    }

    @Test
    void shouldRejectBlankName() {
        var command = new CreateProductCommand("   ", "BOOKS");
        assertThrows(IllegalArgumentException.class, () -> useCase.execute(command));
    }

    // Test double — captures events for assertions without Mockito
    static class CapturingEventPublisher implements DomainEventPublisher {
        final List<DomainEvent> captured = new ArrayList<>();

        @Override
        public void publish(DomainEvent event) {
            captured.add(event);
        }
    }

    // Test double — no-op metrics so unit tests have no Micrometer dependency
    enum NoOpMetrics implements ProductCatalogMetrics {
        INSTANCE;

        @Override public void recordProductCreated() {}
        @Override public void recordProductPublished() {}
        @Override public void recordDomainEventPublished(String eventType) {}
    }
}
