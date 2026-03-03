package yandex.workshop.transferservice.client;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import yandex.workshop.api.model.AccountOwnerResponse;
import yandex.workshop.api.model.OperationResponse;
import yandex.workshop.api.model.TransferRequest;

@Component
public class AccountsClient {

    private final WebClient accountsWebClient;

    public AccountsClient(@Qualifier("accountsWebClient")WebClient accountsWebClient) {
        this.accountsWebClient = accountsWebClient;
    }

    public OperationResponse transfer(TransferRequest request) {
        return accountsWebClient
            .post()
            .uri("/accounts/transfer")
            .bodyValue(request)
            .retrieve()
            .bodyToMono(OperationResponse.class)
            .block();
    }

    public boolean isOwner(String accountLogin, String username) {
        return Boolean.TRUE.equals(accountsWebClient
            .get()
            .uri("/accounts/{accountLogin}/owner", accountLogin)
            .retrieve()
            .bodyToMono(AccountOwnerResponse.class)
            .map(resp -> username.equals(resp.getOwnerUsername()))
            .onErrorReturn(false)
            .block());
    }
}
