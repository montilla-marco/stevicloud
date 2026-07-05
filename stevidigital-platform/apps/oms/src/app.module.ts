import { Module } from '@nestjs/common';

/**
 * Root module — stub for Episode 3.
 * Modules will be added as bounded context features are implemented:
 * - OrderModule (Episode 3)
 * - SagaModule (Episode 3 — checkout orchestration)
 * - KafkaModule (Episode 3 — consume ProductPublished)
 */
@Module({
  imports: [],
  controllers: [],
  providers: [],
})
export class AppModule {}
