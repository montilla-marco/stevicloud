package org.stevidigital.productcatalog.infrastructure.messaging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.stevidigital.productcatalog.application.usecase.DomainEventPublisher;
import org.stevidigital.productcatalog.application.usecase.ProductCatalogMetrics;
import org.stevidigital.productcatalog.domain.event.DomainEvent;
import org.springframework.stereotype.Component;

/**
 * ADAPTER — Stage 1 event publisher. Logs events to stdout instead of Kafka.
 *
 * In Episode 2 this gets replaced by KafkaDomainEventPublisher without touching
 * a single line of domain or application code — that is the payoff of Hexagonal
 * Architecture. [Ford-FSA] Ch.8.
 *
 * The log output is structured so Loki (via OTel Collector) can parse it:
 * event.type and event.id become searchable Loki labels in Grafana.
 * [Majors] Ch.6: "High-cardinality structured logs are the foundation of
 * observability." Use key=value pairs, not free-form strings.
 */
@Component
public class LoggingDomainEventPublisher implements DomainEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(LoggingDomainEventPublisher.class);

    private final ProductCatalogMetrics metrics;

    public LoggingDomainEventPublisher(ProductCatalogMetrics metrics) {
        this.metrics = metrics;
    }

    @Override
    public void publish(DomainEvent event) {
        var eventType = event.getClass().getSimpleName();
        log.info("domain.event.published event.type={} event.id={} occurred_on={}",
                eventType,
                event.eventId(),
                event.occurredOn());
        metrics.recordDomainEventPublished(eventType);
    }
}
