package com.example.demo.payment.application.port;

import com.example.demo.payment.domain.Payment;

import java.util.UUID;

public interface PaymentUseCase {

    Payment.Snapshot prepare(String orderId, long expectedAmount);

    Payment.Snapshot confirm(UUID paymentId, String paymentKey, String idempotencyKey);

    Payment.Snapshot reconcileCancellation(UUID paymentId);

    Payment.Snapshot reconcileWebhook(String eventId, UUID paymentId, String paymentKey);

    Payment.Snapshot get(UUID paymentId);
}
