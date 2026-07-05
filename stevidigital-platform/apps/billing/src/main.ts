import { NestFactory } from '@nestjs/core';
import { AppModule } from './app.module';

/**
 * Billing Service
 *
 * Bounded Context: Billing
 *
 * Episode 4 responsibilities:
 * - Consume OrderConfirmed events from OMS (Kafka)
 * - Process payment charge via payment gateway (Stripe adapter in infrastructure)
 * - Issue invoices and publish PaymentProcessed / PaymentFailed events
 * - Demonstrate event versioning — ProductPublished v1 vs v2 schema evolution
 *
 * [Kleppmann] Ch.4: "Schema evolution" — forward/backward compatibility.
 * When ProductCatalog adds a `description` field to ProductPublished,
 * Billing must handle both old events (no description) and new events.
 * Avro + Schema Registry (Confluent) enforces compatibility rules.
 *
 * [EIP] Dead Letter Channel pattern — failed payment attempts go to
 * billing.payments.dlq for manual review, never silently dropped.
 */
async function bootstrap() {
  const app = await NestFactory.create(AppModule);
  app.setGlobalPrefix('api/v1');
  await app.listen(process.env['PORT'] ?? 3002);
}

bootstrap();
