package yandex.workshop.transferservice.service;

import java.time.Instant;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Service;
import yandex.workshop.api.model.NotificationRequest;
import yandex.workshop.api.model.TransferRequest;
import yandex.workshop.sharedkafka.NotificationProducer;
import yandex.workshop.transferservice.client.AccountsClient;

@Slf4j
@Service
@RequiredArgsConstructor
public class TransferService {

    private final AccountsClient accountsClient;

    private final NotificationProducer notificationProducer;

    @Value("${spring.application.name}")
    private String serviceName;

    @Value("${topic.notification}")
    private String topicName;

    public String submit(TransferRequest request, JwtAuthenticationToken authentication) {
        String username = authentication.getToken().getClaimAsString("preferred_username");

        log.info("Запрос перевода: user={}, from={}, to={}, amount={}",
            username, request.getFromLogin(), request.getToLogin(), request.getAmount());

        boolean owner = accountsClient.isOwner(request.getFromLogin(), username);
        log.debug("Проверка владельца счёта: user={}, from={}, isOwner={}",
            username, request.getFromLogin(), owner);

        if (!owner) {
            log.warn("Отказ в переводе: пользователь {} не владелец счёта {}", username, request.getFromLogin());
            sendNotification("Попытка несанкционированного перевода со счёта "
                + request.getFromLogin() + " пользователем " + username);
            throw new AccessDeniedException(
                "Пользователь " + username + " не является владельцем счёта " + request.getFromLogin()
            );

        }

        String result = accountsClient.transfer(request);

        log.info("Перевод выполнен успешно: {}", result);
        sendNotification("Пользователь " + username + " выполнил перевод со счёта "
            + request.getFromLogin() + " на счёт " + request.getToLogin()
            + " на сумму " + request.getAmount());
        return result;
    }

    private void sendNotification(String message) {
        notificationProducer.send(
            new NotificationRequest()
                .serviceName(serviceName)
                .message(message)
                .timestamp(Instant.now()), topicName);
    }
}