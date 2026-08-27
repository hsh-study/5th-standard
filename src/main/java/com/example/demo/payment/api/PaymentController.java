package com.example.demo.payment.api;

import com.example.demo.payment.application.port.PaymentUseCase;
import com.example.demo.payment.domain.Payment;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/payments")
public class PaymentController {
    private final PaymentUseCase paymentUseCase;

    public PaymentController(PaymentUseCase paymentUseCase) {
        this.paymentUseCase = paymentUseCase;
    }

    @PostMapping
    public Payment.Snapshot prepare(@Valid @RequestBody PrepareRequest request) {
        return paymentUseCase.prepare(request.orderId(), request.expectedAmount());
    }

    @PostMapping("/{paymentId}/confirm")
    public Payment.Snapshot confirm(
        @PathVariable UUID paymentId,
        @RequestHeader("Idempotency-Key") String idempotencyKey,
        @Valid @RequestBody ConfirmRequest request
    ) {
        return paymentUseCase.confirm(paymentId, request.paymentKey(), idempotencyKey);
    }

    @GetMapping("/{paymentId}")
    public Payment.Snapshot get(@PathVariable UUID paymentId) {
        return paymentUseCase.get(paymentId);
    }

    public record PrepareRequest(@NotBlank String orderId, @Positive long expectedAmount) {
    }

    public record ConfirmRequest(@NotBlank String paymentKey) {
    }
}
