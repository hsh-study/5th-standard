package com.example.demo.payment.application;

import com.example.demo.payment.application.port.PaymentUseCase;
import com.example.demo.payment.domain.Payment;
import com.example.demo.payment.domain.PaymentRepository;
import com.example.demo.payment.domain.PaymentStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
public class PaymentService implements PaymentUseCase {

    private final PaymentRepository paymentRepository;
    private final PaymentGateway paymentGateway;
    private final Map<String, Payment.Snapshot> idempotentResults = new ConcurrentHashMap<>();
    private final Set<String> processedWebhookEvents = ConcurrentHashMap.newKeySet();


    @Override
    public Payment.Snapshot prepare(String orderId, long expectedAmount) {
        return paymentRepository.save(Payment.ready(orderId, expectedAmount)).snapshot();
    }

    @Override
    public Payment.Snapshot confirm(UUID paymentId, String paymentKey, String idempotencyKey) {
        return idempotentResults.computeIfAbsent(idempotencyKey, ignored -> confirmOnce(paymentId, paymentKey));
    }

    private Payment.Snapshot confirmOnce(UUID paymentId, String paymentKey) {
        Payment payment = getPayment(paymentId);
        PaymentGateway.Approval approval = paymentGateway.findApproval(paymentKey);
        if (!approval.paid()) {
            payment.fail("PG_NOT_PAID");
            return payment.snapshot();
        }
        try {
            payment.approve(approval.paymentKey(), approval.amount());
        } catch (Payment.AmountMismatchException mismatch) {
            payment.requestCompensationCancel(approval.paymentKey(), "AMOUNT_MISMATCH");
            return cancelAndConverge(payment, approval.paymentKey());
        }
        return payment.snapshot();
    }

    @Override
    public Payment.Snapshot reconcileCancellation(UUID paymentId) {
        Payment payment = getPayment(paymentId);
        Payment.Snapshot current = payment.snapshot();
        if (current.status() == PaymentStatus.CANCELLED) {
            return current;
        }
        if (current.status() != PaymentStatus.CANCEL_PENDING) {
            throw new IllegalStateException("취소 재조정 대상이 아닙니다: " + current.status());
        }
        return cancelAndConverge(payment, current.paymentKey());
    }

    private Payment.Snapshot cancelAndConverge(Payment payment, String paymentKey) {
        try {
            paymentGateway.cancel(paymentKey, "amount-mismatch-" + payment.snapshot().id());
            payment.completeCancel();
        } catch (PaymentGateway.UncertainResultException uncertain) {
            // 결과 미확정은 실패가 아니다. CANCEL_PENDING을 유지하고 조회·재시도로 처리한다.
        }
        return payment.snapshot();
    }

    @Override
    public Payment.Snapshot reconcileWebhook(String eventId, UUID paymentId, String paymentKey) {
        if (!processedWebhookEvents.add(eventId)) {
            return getPayment(paymentId).snapshot();
        }
        Payment payment = getPayment(paymentId);
        PaymentGateway.Approval approval = paymentGateway.findApproval(paymentKey);
        payment.approve(approval.paymentKey(), approval.amount());
        return payment.snapshot();
    }

    @Override
    public Payment.Snapshot get(UUID paymentId) {
        return getPayment(paymentId).snapshot();
    }

    private Payment getPayment(UUID paymentId) {
        return paymentRepository.findById(paymentId)
            .orElseThrow(() -> new IllegalArgumentException("결제를 찾을 수 없습니다: " + paymentId));
    }
}
