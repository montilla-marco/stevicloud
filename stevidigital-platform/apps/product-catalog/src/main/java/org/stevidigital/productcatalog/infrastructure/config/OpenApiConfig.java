package org.stevidigital.productcatalog.infrastructure.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    @Value("${spring.application.name}")
    private String appName;

    @Bean
    public OpenAPI productCatalogOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("ProductCatalog API")
                        .version("1.0.0")
                        .description("""
                                **ProductCatalog Bounded Context** — DDD + Hexagonal Architecture (Episode 01).

                                Manages the product lifecycle: `DRAFT → PUBLISHED → DISCONTINUED`.

                                Each state transition is enforced by the `Product` aggregate root.
                                Domain events (`ProductCreated`, `ProductPublished`, `ProductDiscontinued`)
                                are fired on each transition and dispatched after persistence.

                                In Episode 02, the event publisher adapter is swapped from logging to Kafka
                                without changing a single line of domain or application code.
                                """)
                        .contact(new Contact()
                                .name("stevidigital platform")
                                .url("https://github.com/montilla-marco/marcomarco-private-cloud")))
                .servers(List.of(
                        new Server().url("http://localhost:8080").description("local"),
                        new Server().url("http://product-catalog.stevidigital.svc:8080").description("cluster")
                ));
    }
}
