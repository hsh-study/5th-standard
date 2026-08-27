package com.example.demo.payment.application;

import com.example.demo.payment.domain.Payment;
import com.example.demo.payment.domain.PaymentStatus;
import com.example.demo.payment.infra.FakePaymentGateway;
import com.example.demo.payment.infra.InMemoryPaymentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PaymentServiceTest {

    private FakePaymentGateway gateway;
    private PaymentService service;

    @BeforeEach
    void setUp() {
        gateway = new FakePaymentGateway();
        service = new PaymentService(new InMemoryPaymentRepository(), gateway);
    }

    @Test
    void 승인금액이_서버금액과_같으면_paid로_전이한다() {

    }

    @Test
    void 승인금액과_서버금액이_다르면_승인하지_않고_보상_취소_한다() {

    }

    @Test
    void 취소_요청에_응답이_없으면_pending을_유지하고_같은_키로_재시도한다() {

    }

    @Test
    void 같은_멱등키를_재전송해도_한_결과만_반환한다() {

    }

    @Test
    void 중복된_웹훅은_처리를_한번만_한다() {

    }

}