package yandex.workshop.transferservice.service;

import io.micrometer.core.instrument.MeterRegistry;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Service;
import yandex.workshop.api.model.NotificationRequest;
import yandex.workshop.api.model.OperationResponse;
import yandex.workshop.api.model.TransferRequest;
import yandex.workshop.sharedkafka.NotificationProducer;
import yandex.workshop.transferservice.client.AccountsClient;

@Slf4j
@Service
@RequiredArgsConstructor
public class TransferService {

    private final AccountsClient accountsClient;

    private final NotificationProducer notificationProducer;

    private final MeterRegistry meterRegistry;

    @Value("${spring.application.name}")
    private String serviceName;

    @Value("${topic.notification}")
    private String topicName;

    public String submit(TransferRequest request, JwtAuthenticationToken authentication) {
        String username = authentication.getToken().getClaimAsString("preferred_username");

        log.info("Transfer started user={} from={} to={} amount={}",
            username,
            request.getFromLogin(),
            request.getToLogin(),
            request.getAmount());

        boolean owner = accountsClient.isOwner(request.getFromLogin(), username);
        log.debug("Ownership check user={} account={} result={}",
            username,
            request.getFromLogin(),
            owner);

        if (!owner) {
            log.warn("Transfer rejected: unauthorized access user={} account={}",
                username,
                request.getFromLogin());

            sendNotification("Попытка несанкционированного перевода со счёта "
                + request.getFromLogin() + " пользователем " + username, request.getFromLogin());

            throw new AccessDeniedException(
                "Пользователь " + username + " не является владельцем счёта " + request.getFromLogin()
            );

        }

        OperationResponse result;
        try {
            log.debug("Calling accounts-service transfer from={} to={} amount={}",
                request.getFromLogin(),
                request.getToLogin(),
                request.getAmount());
            result = accountsClient.transfer(request);

        } catch (Exception ex) {
            log.error("Transfer failed due to service error from={} to={} amount={} error={}",
                request.getFromLogin(),
                request.getToLogin(),
                request.getAmount(),
                ex.getMessage(),
                ex);
            throw ex;
        }

        if (!result.getSuccess()) {
            log.warn("Transfer business failure user={} from={} to={} amount={} reason={}",
                username,
                request.getFromLogin(),
                request.getToLogin(),
                request.getAmount(),
                result.getMessage());

            meterRegistry.counter(
                "business.transfer.failed",
                "from", request.getFromLogin(),
                "to", request.getToLogin()
            ).increment();

            sendNotification("Ошибка при выполнении перевода со счёта "
                + request.getFromLogin() + " на счёт " + request.getToLogin()
                + " пользователем " + username + ": " + result.getMessage(), request.getFromLogin());

        } else {
            log.info("Transfer completed user={} from={} to={} amount={}",
                username,
                request.getFromLogin(),
                request.getToLogin(),
                request.getAmount());
            sendNotification("Пользователь " + username + " выполнил перевод со счёта "
                + request.getFromLogin() + " на счёт " + request.getToLogin()
                + " на сумму " + request.getAmount(), request.getFromLogin());
        }

        return result.getMessage();
    }

    private void sendNotification(String message, String login) {
        log.debug("Sending notification login={} topic={} message={}",
            login,
            topicName,
            message);
        notificationProducer.send(
            new NotificationRequest()
                .serviceName(serviceName)
                .message(message)
                .timestamp(Instant.now())
                .login(login), topicName);
    }
}