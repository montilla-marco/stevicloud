/**
 * Shared event contracts — the Published Language for the stevidigital platform.
 *
 * [Evans] Ch.14 Context Map — Published Language:
 * "Use a well-documented shared language that can express the necessary domain
 * information as a common medium of communication, translating as necessary
 * into and out of that language."
 *
 * These types are the language-agnostic contract. The Java side uses Avro/JSON
 * schemas; the TypeScript side imports these types. Both must agree on field names.
 *
 * In Episode 4 these will be backed by Avro schemas registered in Confluent
 * Schema Registry, enabling:
 * - Forward compatibility (new fields with defaults — old consumers still work)
 * - Backward compatibility (removed fields — new consumers handle missing data)
 *
 * [Kleppmann] Ch.4 p.122: "Avro is the schema evolution champion among the
 * binary encoding formats."
 */

// ── ProductCatalog events ─────────────────────────────────────────────────────

export interface DomainEvent {
  eventId: string;
  occurredOn: string; // ISO-8601 instant
}

export interface ProductCreated extends DomainEvent {
  type: 'ProductCreated';
  productId: string;
  name: string;
  category: string;
}

export interface ProductPublished extends DomainEvent {
  type: 'ProductPublished';
  productId: string;
}

export interface ProductDiscontinued extends DomainEvent {
  type: 'ProductDiscontinued';
  productId: string;
}

// ── OMS events ────────────────────────────────────────────────────────────────

export interface OrderCreated extends DomainEvent {
  type: 'OrderCreated';
  orderId: string;
  customerId: string;
  lineItems: Array<{ productId: string; quantity: number; unitPrice: number }>;
}

export interface OrderConfirmed extends DomainEvent {
  type: 'OrderConfirmed';
  orderId: string;
  totalAmount: number;
  currency: string;
}

export interface OrderFailed extends DomainEvent {
  type: 'OrderFailed';
  orderId: string;
  reason: string;
}

// ── Billing events ────────────────────────────────────────────────────────────

export interface PaymentProcessed extends DomainEvent {
  type: 'PaymentProcessed';
  orderId: string;
  invoiceId: string;
  amount: number;
  currency: string;
}

export interface PaymentFailed extends DomainEvent {
  type: 'PaymentFailed';
  orderId: string;
  reason: string;
}
