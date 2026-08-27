package com.example.demo.payment.domain;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

public class Payment {
    private UUID id;
    private String orderId;
    private long expectedAmount;
    private LocalDateTime createdAt;
    private PaymentStatus status;
    private String paymentKey;
    private String failureReason;

    private Payment(UUID id, String orderId, long expectedAmount) {
        if (expectedAmount < 0) {
            throw new IllegalArgumentException("결제 금액은 0원 보다 커야 합니다.");
        }

        this.id = Objects.requireNonNull(id);
        this.orderId = Objects.requireNonNull(orderId);
        this.expectedAmount = expectedAmount;
        this.createdAt = LocalDateTime.now();
        this.status = PaymentStatus.READY;
    }

    public static Payment ready(String orderId, long expectedAmount) {
        return new Payment(UUID.randomUUID(), orderId, expectedAmount);
    }

    public synchronized void approve(String paymentKey, long approvedAmount) {
        if (this.status == PaymentStatus.PAID
            && Objects.equals(this.paymentKey, paymentKey)
            && expectedAmount == approvedAmount) {
            return;
        }

        requireStatus(PaymentStatus.READY);

        if (expectedAmount != approvedAmount) {
            throw new AmountMismatchException(expectedAmount, approvedAmount);
        }

        this.paymentKey = Objects.requireNonNull(paymentKey);
        this.status = PaymentStatus.PAID;
    }

    public synchronized void fail(String reason) {
        requireStatus(PaymentStatus.READY);
        this.failureReason = Objects.requireNonNull(reason);
        this.status = PaymentStatus.FAILED;
    }

    public synchronized void requestCancel() {
        requireStatus(PaymentStatus.PAID);
        this.status = PaymentStatus.CANCELLED;
    }

    public synchronized void requestCompensationCancel(String paymentKey, String reason) {
        requireStatus(PaymentStatus.READY);
        this.paymentKey = Objects.requireNonNull(paymentKey);
        this.failureReason = Objects.requireNonNull(reason);
        this.status = PaymentStatus.CANCEL_PENDING;
    }

    public synchronized void completeCancel() {
        if (this.status == PaymentStatus.CANCELLED) {
            return;
        }
        requireStatus(PaymentStatus.CANCEL_PENDING);
        this.status = PaymentStatus.CANCELLED;
    }

    private void requireStatus(PaymentStatus expectedStatus) {
        if (this.status != expectedStatus) {
            throw new IllegalStateException("허용되지 않는 결제 상태 - 현재 : " + this.status + ", 기대 : " + expectedStatus);
        }
    }

    public synchronized Snapshot snapshot() {
        return new Snapshot(id, orderId, expectedAmount, status, paymentKey, failureReason, createdAt);
    }

    public record Snapshot(
        UUID id,
        String orderId,
        long expectedAmount,
        PaymentStatus status,
        String paymentKey,
        String failureReason,
        LocalDateTime createdAt
    ) {}

    public static final class AmountMismatchException extends RuntimeException {
        public AmountMismatchException(long expected, long actual) {
            super("결제 금액 불일치: expected=" + expected + ", actual=" + actual);
        }
    }

    public PaymentStatus getStatus() {
        return this.status;
    }

    public void changeStatus(PaymentStatus status) {
        this.status = status;
    }
}
