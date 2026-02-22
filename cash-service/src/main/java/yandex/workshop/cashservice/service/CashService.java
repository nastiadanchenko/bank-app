package yandex.workshop.cashservice.service;

import java.time.Instant;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import yandex.workshop.cashservice.api.accounts.model.CashRequest;
import yandex.workshop.cashservice.api.accounts.model.NotificationRequest;
import yandex.workshop.cashservice.client.AccountsClient;

@Slf4j
@Service
@RequiredArgsConstructor
public class CashService {

    private final AccountsClient accountsClient;

//    public final NotificationClient notificationClient;

    private final NotificationProducer notificationProducer;

    @Value("${spring.application.name}")
    public String serviceName;


    public String submit(CashRequest request) {

        String result = accountsClient.sendTransaction(request);

        log.info("Операция с наличными: {}", result);

        sendNotification("Cash operation " + request.getAction() +
            " of " + request.getValue() +
            " for " + request.getAccountLogin(), request.getAccountId()
            );

        return result;
    }


    private void sendNotification(String message, String userId) {
        notificationProducer.send(
            new NotificationRequest()
                .serviceName(serviceName)
                .message(message)
                .timestamp(Instant.now())
                .userId(userId));
    }
}
