plugins {
    id("org.springframework.boot")
}

dependencies {
    // Web + validation
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-validation")

    // Actuator + Observability
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("io.micrometer:micrometer-tracing-bridge-otel")
    implementation("io.opentelemetry:opentelemetry-exporter-otlp")
    implementation("io.micrometer:micrometer-registry-prometheus")

    // OpenAPI / Swagger UI (disabled in prod via application-prod.yaml)
    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:2.8.4")

    // Spring Events (in-process domain events — Stage 1)
    // Kafka will replace this in Stage 2
    implementation("org.springframework:spring-context")

    // Test
    testImplementation("org.springframework.boot:spring-boot-starter-test")
}
