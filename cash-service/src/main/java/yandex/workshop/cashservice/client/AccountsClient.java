package yandex.workshop.cashservice.client;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import yandex.workshop.cashservice.api.accounts.model.CashRequest;

@Component
public class AccountsClient {

    private final WebClient accountsWebClient;

    public AccountsClient(@Qualifier("accountsWebClient")WebClient accountsWebClient) {
        this.accountsWebClient = accountsWebClient;
    }

    public String sendTransaction(CashRequest request) {
        return accountsWebClient
            .post()
            .uri("/accounts/cash")
            .bodyValue(request)
            .retrieve()
            .bodyToMono(String.class)
            .onErrorResume(throwable -> Mono.just("Ошибка при обращении к accounts-service: " + throwable.getMessage()))
            .block();
    }


}
