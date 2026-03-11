package yandex.workshop.cashservice.client;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import yandex.workshop.api.model.CashRequest;
import yandex.workshop.api.model.OperationResponse;

@Component
public class AccountsClient {

    private final WebClient accountsWebClient;

    public AccountsClient(@Qualifier("accountsWebClient")WebClient accountsWebClient) {
        this.accountsWebClient = accountsWebClient;
    }

    public OperationResponse sendTransaction(CashRequest request) {
        return accountsWebClient
            .post()
            .uri("/accounts/cash")
            .bodyValue(request)
            .retrieve()
            .bodyToMono(OperationResponse.class)
            .block();
    }


}
