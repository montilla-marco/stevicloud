package org.stevidigital.productcatalog.infrastructure.config;

import org.stevidigital.productcatalog.application.usecase.*;
import org.stevidigital.productcatalog.domain.repository.ProductRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Spring wiring — explicit @Bean declarations instead of @Service/@Component on
 * use cases, to keep the application layer free of Spring annotations.
 *
 * [Evans] Ch.4: Domain and application layers should not depend on the framework.
 * Spring annotations on use cases would make them untestable without a Spring context.
 * Here we push that coupling to the config class, which is an infrastructure concern.
 */
@Configuration
public class ApplicationConfig {

    @Bean
    public CreateProductUseCase createProductUseCase(ProductRepository repository,
                                                     DomainEventPublisher eventPublisher,
                                                     ProductCatalogMetrics metrics) {
        return new CreateProductUseCase(repository, eventPublisher, metrics);
    }

    @Bean
    public PublishProductUseCase publishProductUseCase(ProductRepository repository,
                                                       DomainEventPublisher eventPublisher,
                                                       ProductCatalogMetrics metrics) {
        return new PublishProductUseCase(repository, eventPublisher, metrics);
    }

    @Bean
    public GetProductUseCase getProductUseCase(ProductRepository repository) {
        return new GetProductUseCase(repository);
    }
}
