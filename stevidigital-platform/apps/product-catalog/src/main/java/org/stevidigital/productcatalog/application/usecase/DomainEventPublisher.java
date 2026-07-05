package org.stevidigital.productcatalog.application.usecase;

import org.stevidigital.productcatalog.domain.event.DomainEvent;

/**
 * Port — the application layer's view of "publish an event somewhere."
 *
 * Stage 1 adapter: LoggingDomainEventPublisher (just logs to stdout)
 * Stage 2 adapter: KafkaDomainEventPublisher   (Episode 2)
 * Stage 3 adapter: OutboxDomainEventPublisher  (transactional outbox, Episode 4+)
 *
 * [EIP] Hohpe & Woolf: Message Channel pattern — the publisher doesn't know the
 * channel type. It just sends. The adapter wires the channel at startup.
 */
public interface DomainEventPublisher {
    void publish(DomainEvent event);
}
