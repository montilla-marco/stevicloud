import { Module } from '@nestjs/common';

/**
 * Root module — stub for Episode 4.
 * Modules to be added:
 * - PaymentModule (Episode 4)
 * - InvoiceModule (Episode 4)
 * - KafkaModule (consume OrderConfirmed, publish PaymentProcessed)
 */
@Module({
  imports: [],
  controllers: [],
  providers: [],
})
export class AppModule {}
