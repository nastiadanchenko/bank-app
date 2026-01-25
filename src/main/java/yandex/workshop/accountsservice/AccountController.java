package yandex.workshop.accountsservice;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import yandex.workshop.account.api.accounts.ApiApi;
import yandex.workshop.account.api.accounts.model.AccountResponse;
import yandex.workshop.account.api.accounts.model.UpdateAccountRequest;

@RestController
@RequestMapping("/accounts")
@RequiredArgsConstructor
public class AccountController implements ApiApi {

    private final AccountService service;
    @GetMapping("/me")
    public AccountResponse me(JwtAuthenticationToken token) {
        Account account = service.getCurrentAccount(token.getName());
        return toDto(account);
    }

    @PutMapping("/me")
    public AccountResponse updateAccount(JwtAuthenticationToken token, UpdateAccountRequest dto) {
        Account account = service.updateProfile(token.getName(), dto);
        return toDto(account);
    }


    @GetMapping("")
    public List<AccountResponse> getAccounts(JwtAuthenticationToken token) {
        List<Account> account = service.getOtherAccounts(token.getName());
        return account.stream().map(this::toDto).toList();
    }

    private AccountResponse toDto(Account account) {
        return new AccountResponse()
            .login(account.getLogin())
            .name(account.getFirstName() + " " + account.getLastName())
            .birthdate(account.getBirthDate());
    }

}
