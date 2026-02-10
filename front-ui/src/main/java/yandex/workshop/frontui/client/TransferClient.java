package yandex.workshop.frontui.client;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import yandex.workshop.frontui.api.accounts.model.OperationResponse;
import yandex.workshop.frontui.api.accounts.model.TransferRequest;

@Component
@RequiredArgsConstructor
public class TransferClient {

    private final WebClient gatewayWebClient;

    @Value("${gateway.url}")
    private String gatewayUrl;

    public OperationResponse transfer(TransferRequest request) {
        return gatewayWebClient.post()
            .uri(gatewayUrl + "/transfers")
            .bodyValue(request)
            .retrieve()
            .bodyToMono(OperationResponse.class)
            .block();
    }
}
