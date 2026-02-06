package yandex.workshop.accountsservice.client;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import yandex.workshop.account.api.accounts.model.NotificationRequest;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationClient {

    private final WebClient notificationWebClient;

    public void notify(NotificationRequest request) {
        notificationWebClient
            .post()
            .uri("/notifications")
            .bodyValue(request)
            .retrieve()
            .toBodilessEntity()
            .block();
    }


}
