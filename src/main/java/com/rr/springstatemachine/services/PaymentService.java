package com.rr.springstatemachine.services;

import com.rr.springstatemachine.domain.Payment;
import com.rr.springstatemachine.domain.PaymentEvent;
import com.rr.springstatemachine.domain.PaymentState;
import org.springframework.statemachine.StateMachine;

public interface PaymentService {

    Payment newPayment(Payment payment);

    StateMachine<PaymentState, PaymentEvent> preAuth(Long paymentId);

    StateMachine<PaymentState, PaymentEvent> authorizePayment(Long paymentId);

    StateMachine<PaymentState, PaymentEvent> declinePayment(Long paymentId);
}
