package com.example.demo.payment.api;

import com.example.demo.payment.application.port.PaymentUseCase;
import com.example.demo.payment.domain.Payment;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/webhooks")
public class PaymentWebhookController {
    private final PaymentUseCase paymentUseCase;

    public PaymentWebhookController(PaymentUseCase paymentUseCase) {
        this.paymentUseCase = paymentUseCase;
    }

    @PostMapping("/payments")
    public Payment.Snapshot paymentCompleted(@Valid @RequestBody WebhookRequest request) {
        return paymentUseCase.reconcileWebhook(request.eventId(), request.paymentId(), request.paymentKey());
    }

    public record WebhookRequest(@NotBlank String eventId, @NotNull UUID paymentId, @NotBlank String paymentKey) {
    }
}
