import { NestFactory } from '@nestjs/core';
import { AppModule } from './app.module';

/**
 * OMS — Order Management Service
 *
 * Bounded Context: Ordering
 *
 * Episode 3 responsibilities:
 * - Receive CreateOrder commands from customers
 * - Orchestrate the Checkout Saga: Reserve Inventory → Charge Payment → Confirm Order
 * - Publish OrderCreated, OrderConfirmed, OrderFailed domain events to Kafka
 *
 * [EIP] Hohpe & Woolf: Process Manager pattern (p.312) — the Saga orchestrator
 * is a stateful process manager that coordinates multiple steps across BCs.
 *
 * [Kleppmann] Ch.9: "Saga vs 2PC" — we choose Saga choreography for simple
 * flows, orchestration for the checkout where we need compensating transactions.
 */
async function bootstrap() {
  const app = await NestFactory.create(AppModule);
  app.setGlobalPrefix('api/v1');
  await app.listen(process.env['PORT'] ?? 3001);
}

bootstrap();
