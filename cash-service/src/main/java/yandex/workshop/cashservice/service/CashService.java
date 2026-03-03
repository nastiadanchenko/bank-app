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
        OperationResponse result = accountsClient.sendTransaction(request);

        log.info("Операция с наличными: {}", result);

        if (!result.getSuccess()) {
            log.error("Ошибка при выполнении операции с наличными: {}", result.getMessage());

            if ("GET" .equals(request.getAction())) {
                meterRegistry.counter(
                    "business.cash.failed",
                    "login", request.getAccountLogin()
                ).increment();
            }

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
        notificationProducer.send(
            new NotificationRequest()
                .serviceName(serviceName)
                .message(message)
                .timestamp(Instant.now())
                .userId(userId)
                .login(login), topicName);
    }
}
