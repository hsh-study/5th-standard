package com.example.demo.payment.application;

import com.example.demo.payment.domain.Payment;
import com.example.demo.payment.domain.PaymentRepository;
import com.example.demo.payment.domain.PaymentStatus;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class PaymentService {

    private final PaymentRepository paymentRepository;

    public PaymentService(PaymentRepository paymentRepository) {
        this.paymentRepository = paymentRepository;
    }

    @Transactional
    public void completeCancel(UUID paymentId) {
        Payment payment = paymentRepository.findById(paymentId).orElseThrow();

        if (payment.getStatus() == PaymentStatus.CANCELLED) {
            return;
        }
        requireStatus(PaymentStatus.CANCEL_PENDING);
        payment.changeStatus(PaymentStatus.CANCELLED);
    }

    private void requireStatus(PaymentStatus expectedStatus) {
        if (this.status != expectedStatus) {
            throw new IllegalStateException("허용되지 않는 결제 상태 - 현재 : " + this.status + ", 기대 : " + expectedStatus);
        }
    }

}
