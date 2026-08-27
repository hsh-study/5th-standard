package com.example.demo.payment.infra;

import com.example.demo.payment.application.PaymentGateway;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class FakePaymentGateway implements PaymentGateway {

    private final Map<String, Approval> approvals = new ConcurrentHashMap<>();
    private final Set<String> cancellationKeys = ConcurrentHashMap.newKeySet();
    private volatile boolean loseNextCancellationResponse;

    public void registerPaid(String paymentKey, long amount) {
        approvals.put(paymentKey, new Approval(paymentKey, amount, true));
    }

    @Override
    public Approval findApproval(String paymentKey) {
        return approvals.getOrDefault(paymentKey, new Approval(paymentKey, 0, false));
    }

    @Override
    public void cancel(String paymentKey, String idempotencyKey) {
        cancellationKeys.add(paymentKey + ":" + idempotencyKey);
        if (loseNextCancellationResponse) {
            loseNextCancellationResponse = false;
            throw new UncertainResultException("PG 취소 응답을 확인하지 못했습니다.");
        }
    }

    public void loseNextCancellationResponse() {
        loseNextCancellationResponse = true;
    }

    public int cancellationCount() {
        return cancellationKeys.size();
    }
}
