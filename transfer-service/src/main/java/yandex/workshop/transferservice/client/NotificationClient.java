package yandex.workshop.transferservice.client;


import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import yandex.workshop.transfer_service.api.accounts.model.NotificationRequest;

@Slf4j
@Component
public class NotificationClient {

    private final WebClient notificationWebClient;

    public NotificationClient(@Qualifier("notificationWebClient") WebClient notificationWebClient) {
        this.notificationWebClient = notificationWebClient;
    }

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
