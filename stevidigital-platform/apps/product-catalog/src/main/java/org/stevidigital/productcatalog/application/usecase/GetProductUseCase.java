package org.stevidigital.productcatalog.application.usecase;

import org.stevidigital.productcatalog.domain.model.Product;
import org.stevidigital.productcatalog.domain.model.ProductId;
import org.stevidigital.productcatalog.domain.repository.ProductRepository;

import java.util.List;
import java.util.Optional;

/**
 * APPLICATION SERVICE — read-only queries for products.
 *
 * [Evans] Ch.4: "QUERY: An operation that returns a result and changes no observable
 * state of the system." CQS (Command-Query Separation) in practice.
 *
 * These never call product.pullDomainEvents() because queries produce no events.
 * In CQRS (Episode 5+), the read side will be its own model with its own projection.
 */
public class GetProductUseCase {

    private final ProductRepository repository;

    public GetProductUseCase(ProductRepository repository) {
        this.repository = repository;
    }

    public Optional<Product> findById(String productId) {
        return repository.findById(ProductId.of(productId));
    }

    public List<Product> findAll() {
        return repository.findAll();
    }
}
