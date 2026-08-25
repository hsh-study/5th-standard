package com.example.demo.payment.infra;

import com.example.demo.payment.domain.Payment;
import com.example.demo.payment.domain.PaymentRepository;
import org.springframework.stereotype.Repository;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Repository
public class InMemoryPaymentRepository implements PaymentRepository {

    private final Map<UUID, Payment> payments = new ConcurrentHashMap<>();

    @Override
    public Payment save(Payment payment) {
        payments.put(payment.snapshot().id(), payment);
        return null;
    }

    @Override
    public Optional<Payment> findById(UUID id) {
        return Optional.ofNullable(payments.get(id));
    }
}
