package com.example.demo.payment.domain;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.*;

class PaymentTest {

    @Test
    void 서버에_Snapshot_된_금액과_결제_승인_금액이_같아야_한다() {
        Payment payment = Payment.ready("order-1", 10_000);

        assertThatThrownBy(() -> payment.approve("pg-1", 100))
            .isInstanceOf(Payment.AmountMismatchException.class);

        assertThat(payment.snapshot().status()).isEqualTo(PaymentStatus.READY);

    }

    @Test
    void 같은_요청은_같은_상태를_응답한다() {

        Payment payment = Payment.ready("order-1", 10_000);

        payment.approve("pg-1", 10_000);
        payment.approve("pg-1", 10_000);

        assertThat(payment.snapshot().status()).isEqualTo(PaymentStatus.PAID);

    }

    @Test
    void 금액_불일치한_결제는_외부_승인을_취소할_때까지_pending() {

        Payment payment = Payment.ready("order-1", 10_000);

        assertThatThrownBy(() -> payment.approve("pg-1", 100))
            .isInstanceOf(Payment.AmountMismatchException.class);

        payment.requestCompensationCancel("pg-1", "AMOUNT_MISMATCH");

        assertThat(payment.snapshot().status()).isEqualTo(PaymentStatus.CANCEL_PENDING);
        assertThat(payment.snapshot().paymentKey()).isEqualTo("pg-1");
        assertThat(payment.snapshot().failureReason()).isEqualTo("AMOUNT_MISMATCH");

        payment.completeCancel();

        assertThat(payment.snapshot().status()).isEqualTo(PaymentStatus.CANCELLED);



    }


}