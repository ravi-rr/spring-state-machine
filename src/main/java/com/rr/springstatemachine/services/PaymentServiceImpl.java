package com.rr.springstatemachine.services;

import com.rr.springstatemachine.domain.Payment;
import com.rr.springstatemachine.domain.PaymentEvent;
import com.rr.springstatemachine.domain.PaymentState;
import com.rr.springstatemachine.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.statemachine.StateMachine;
import org.springframework.statemachine.config.StateMachineFactory;
import org.springframework.statemachine.support.DefaultStateMachineContext;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
public class PaymentServiceImpl implements PaymentService {

    public static final String PAYMENT_ID_HEADER = "payment_id";

    private final PaymentRepository paymentRepository;
    private final StateMachineFactory<PaymentState, PaymentEvent> stateMachineFactory;

    @Override
    public Payment newPayment(Payment payment) {
        payment.setState(PaymentState.NEW);
        return paymentRepository.save(payment);
    }

    @Override
    public StateMachine<PaymentState, PaymentEvent> preAuth(Long paymentId) {
        StateMachine<PaymentState, PaymentEvent> sm = build(paymentId);

        sendEvent(paymentId, sm, PaymentEvent.PRE_AUTHORIZE);
        return null;
    }

    @Override
    public StateMachine<PaymentState, PaymentEvent> authorizePayment(Long paymentId) {
        StateMachine<PaymentState, PaymentEvent> sm = build(paymentId);

        sendEvent(paymentId, sm, PaymentEvent.AUTH_APPROVED);
        return null;
    }

    @Override
    public StateMachine<PaymentState, PaymentEvent> declinePayment(Long paymentId) {
        StateMachine<PaymentState, PaymentEvent> sm = build(paymentId);

        sendEvent(paymentId, sm, PaymentEvent.AUTH_DECLINED);
        return null;
    }

    private void sendEvent(Long paymentId, StateMachine<PaymentState, PaymentEvent> sm, PaymentEvent event) {
        Message msg = MessageBuilder.withPayload(event)
                .setHeader(PAYMENT_ID_HEADER, paymentId)
                .build();

        sm.sendEvent(msg);
    }

    private StateMachine<PaymentState, PaymentEvent> build(Long paymentId) {
        // get payment from db
        Payment payment = paymentRepository.getOne(paymentId);

        // get state machine instance
        StateMachine<PaymentState, PaymentEvent> sm = stateMachineFactory.getStateMachine(Long.toString(payment.getId()));

        // stop it
        sm.stop();

        // restore state machine state (not reset as in previous commit)
        sm.getStateMachineAccessor()
                .doWithAllRegions(sma -> {
                    sma.resetStateMachine(new DefaultStateMachineContext<>(payment.getState(), null, null, null));
                });

        // start it
        sm.start();

        return sm;
    }
}
