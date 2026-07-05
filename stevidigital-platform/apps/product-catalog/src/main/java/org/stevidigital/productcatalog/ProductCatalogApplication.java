package org.stevidigital.productcatalog;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Entry point for the ProductCatalog bounded context.
 *
 * This is the ONLY class in the entire service that knows about Spring Boot.
 * Every other class is either:
 * - A pure domain object (no framework imports)
 * - A use case (depends only on domain)
 * - An adapter (depends on the port interface, not other adapters)
 * - A Spring @Configuration / @Component wiring the adapters together
 *
 * [Ford-FSA] Ch.8: Hexagonal Architecture gives you this freedom.
 * If we ever need to run the domain logic without a web server (e.g., in a
 * CLI migration tool or a Kafka consumer), we'd just write a different main class
 * that wires different adapters — the domain and application layers are untouched.
 */
@SpringBootApplication
public class ProductCatalogApplication {

    public static void main(String[] args) {
        SpringApplication.run(ProductCatalogApplication.class, args);
    }
}
