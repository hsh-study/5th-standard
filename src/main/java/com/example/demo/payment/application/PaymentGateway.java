package com.example.demo.payment.application;

public interface PaymentGateway {

    Approval findApproval(String paymentKey);

    void cancel(String paymentKey, String idempotencyKey);

    record Approval(String paymentKey, long amount, boolean paid) {
    }

    final class UncertainResultException extends RuntimeException {
        public UncertainResultException(String message) {
            super(message);
        }
    }
}
