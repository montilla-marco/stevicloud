package org.stevidigital.productcatalog.infrastructure.persistence;

import org.stevidigital.productcatalog.domain.model.Product;
import org.stevidigital.productcatalog.domain.model.ProductId;
import org.stevidigital.productcatalog.domain.repository.ProductRepository;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * ADAPTER — Stage 1 implementation of ProductRepository (the port).
 *
 * [Evans] Ch.6 p.153: "For each type of object that needs global access, create
 * an object that can provide the illusion of an in-memory collection."
 * Here it literally IS an in-memory collection — and that's fine for Stage 1.
 *
 * [Ford-FSA] Ch.8 Hexagonal Architecture: This adapter lives in infrastructure,
 * depends on the domain port (ProductRepository), and is wired by Spring.
 * Swapping this for JpaProductRepository in Stage 2 requires:
 * - No changes to domain layer
 * - No changes to application layer
 * - New @Repository class + disable this one (or use @ConditionalOnProperty)
 *
 * Thread safety: ConcurrentHashMap — safe for concurrent Spring MVC requests.
 */
@Repository
public class InMemoryProductRepository implements ProductRepository {

    private final Map<String, Product> store = new ConcurrentHashMap<>();

    @Override
    public void save(Product product) {
        store.put(product.id().value(), product);
    }

    @Override
    public Optional<Product> findById(ProductId id) {
        return Optional.ofNullable(store.get(id.value()));
    }

    @Override
    public List<Product> findAll() {
        return new ArrayList<>(store.values());
    }

    @Override
    public boolean existsById(ProductId id) {
        return store.containsKey(id.value());
    }
}
