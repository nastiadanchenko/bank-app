package yandex.workshop.cashservice.service;

import io.micrometer.core.instrument.MeterRegistry;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import yandex.workshop.api.model.CashRequest;
import yandex.workshop.api.model.NotificationRequest;
import yandex.workshop.api.model.OperationResponse;
import yandex.workshop.cashservice.client.AccountsClient;
import yandex.workshop.sharedkafka.NotificationProducer;

@Slf4j
@Service
@RequiredArgsConstructor
public class CashService {

    private final AccountsClient accountsClient;

    private final NotificationProducer notificationProducer;

    private final MeterRegistry meterRegistry;

    @Value("${spring.application.name}")
    public String serviceName;

    @Value("${topic.notification}")
    private String topicName;


    public String submit(CashRequest request) {
        log.info("Cash operation started action={} login={} amount={}",
            request.getAction(),
            request.getAccountLogin(),
            request.getValue());
        OperationResponse result;
        try {
            result = accountsClient.sendTransaction(request);
        } catch (RuntimeException e) {
            log.error("Cash operation failed due to service error action={} login={} amount={} error={}",
                request.getAction(),
                request.getAccountLogin(),
                request.getValue(),
                e.getMessage(),
                e);
            throw e;
        }
        log.debug("Accounts service response success={} message={}",
            result.getSuccess(),
            result.getMessage());

        if (!result.getSuccess()) {
            log.debug("Accounts service response success={} message={}",
                result.getSuccess(),
                result.getMessage());

            if ("GET" .equals(request.getAction())) {
                meterRegistry.counter(
                    "business.cash.failed",
                    "login", request.getAccountLogin()
                ).increment();
            }
        } else {
            log.info("Cash operation completed action={} login={} amount={}",
                request.getAction(),
                request.getAccountLogin(),
                request.getValue());
        }

        sendNotification("Cash operation " + request.getAction() +
                " of " + request.getValue() +
                " for " + request.getAccountLogin(), request.getAccountId() +
                " result: " + result.getMessage() + " success: " + result.getSuccess(),
            request.getAccountLogin()
        );

        return result.getMessage();
    }

    private void sendNotification(String message, String userId, String login) {
        log.debug("Sending notification user={} topic={} message={}",
            login,
            topicName,
            message);
        notificationProducer.send(
            new NotificationRequest()
                .serviceName(serviceName)
                .message(message)
                .timestamp(Instant.now())
                .userId(userId)
                .login(login), topicName);

    }
}
