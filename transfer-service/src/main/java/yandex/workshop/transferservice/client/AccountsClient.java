package yandex.workshop.transferservice.client;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import yandex.workshop.transfer_service.api.accounts.model.AccountOwnerResponse;
import yandex.workshop.transfer_service.api.accounts.model.TransferRequest;

@Component
public class AccountsClient {

    private final WebClient accountsWebClient;

    public AccountsClient(@Qualifier("accountsWebClient")WebClient accountsWebClient) {
        this.accountsWebClient = accountsWebClient;
    }

    public String transfer(TransferRequest request) {
        return accountsWebClient
            .post()
            .uri("/accounts/transfer")
            .bodyValue(request)
            .retrieve()
            .bodyToMono(String.class)
            .onErrorResume(throwable -> Mono.just("Ошибка при обращении к accounts-service: " + throwable.getMessage()))
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
