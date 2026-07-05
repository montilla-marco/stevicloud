package org.stevidigital.productcatalog.domain.repository;

import org.stevidigital.productcatalog.domain.model.Product;
import org.stevidigital.productcatalog.domain.model.ProductId;

import java.util.List;
import java.util.Optional;

/**
 * REPOSITORY — Port (interface) in Hexagonal Architecture terms.
 *
 * [Evans] Ch.6 p.152: "For each type of object that needs global access, create an
 * object that can provide the illusion of an in-memory collection of all objects
 * of that type."
 *
 * This interface lives in the DOMAIN layer. It knows nothing about SQL, JPA, or HTTP.
 * The implementation (adapter) lives in the infrastructure layer and is injected by
 * Spring at runtime.
 *
 * [Ford-FSA] Ch.8 — Hexagonal Architecture (Ports & Adapters):
 * The domain defines the port (what it needs), not how it's provided.
 * This lets us swap InMemoryProductRepository (Stage 1) for JpaProductRepository
 * (Stage 2) without touching a single line of domain or application code.
 */
public interface ProductRepository {

    void save(Product product);

    Optional<Product> findById(ProductId id);

    List<Product> findAll();

    boolean existsById(ProductId id);
}
