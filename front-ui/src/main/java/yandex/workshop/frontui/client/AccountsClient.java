package yandex.workshop.frontui.client;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import yandex.workshop.frontui.api.accounts.model.UpdateAccountRequest;
import yandex.workshop.frontui.dto.AccountDto;

@Component
@RequiredArgsConstructor
public class AccountsClient {
    private final WebClient webClient;

    @Value("${gateway.url}")
    private String gatewayUrl;

    public AccountDto getMyAccount() {
        return webClient.get()
            .uri(gatewayUrl + "/accounts/me")
            .retrieve()
            .bodyToMono(AccountDto.class)
            .block();
    }


    public void updateAccount(UpdateAccountRequest request) {
        webClient.post()
            .uri(gatewayUrl + "/accounts/me")
            .bodyValue(request)
            .retrieve()
            .toBodilessEntity()
            .block();
    }

    public List<AccountDto> getOtherAccounts() {
        return webClient.get()
            .uri(gatewayUrl + "/accounts")
            .retrieve()
            .bodyToFlux(AccountDto.class)
            .collectList()
            .block();
    }
}

