package yandex.workshop.frontui.client;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import yandex.workshop.frontui.api.accounts.model.CashRequest;
import yandex.workshop.frontui.api.accounts.model.OperationResponse;

@Component
@RequiredArgsConstructor
public class CashClient {
    private final WebClient webClient;

    @Value("${gateway.url}")
    private String gatewayUrl;

    public OperationResponse cash(CashRequest request) {
        return webClient.post()
            .uri(gatewayUrl + "/cash")
            .bodyValue(request)
            .retrieve()
            .bodyToMono(OperationResponse.class)
            .block();
    }
}
