package yandex.workshop.accountsservice.controller;

import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import yandex.workshop.account.api.accounts.ApiApi;
import yandex.workshop.account.api.accounts.model.AccountOwnerResponse;
import yandex.workshop.account.api.accounts.model.AccountResponse;
import yandex.workshop.account.api.accounts.model.CashRequest;
import yandex.workshop.account.api.accounts.model.TransferRequest;
import yandex.workshop.account.api.accounts.model.UpdateAccountRequest;
import yandex.workshop.accountsservice.service.AccountService;

@Slf4j
@RestController
@RequestMapping("/accounts")
@RequiredArgsConstructor
public class AccountController implements ApiApi {

    private final AccountService accountService;

    @GetMapping("/me")
    @PreAuthorize("hasRole('SERVICE')")
    public AccountResponse getOwnerAccount(JwtAuthenticationToken token) {
        return accountService.getCurrentAccount(token);
    }

    @PostMapping("/me")
    @PreAuthorize("hasRole('SERVICE') && hasAuthority('accounts.write')")
    public AccountResponse updateAccount(JwtAuthenticationToken token,
                                         @RequestBody UpdateAccountRequest dto) {
        log.debug("Authorities in PUT: {}", token.getAuthorities());
        return accountService.updateProfile(token.getToken().getSubject(), dto);
    }


    @GetMapping("")
    public List<AccountResponse> getAccounts(JwtAuthenticationToken token) {
        return accountService.getOtherAccounts(token.getName());
    }

    @PostMapping("/cash")
    @PreAuthorize("hasRole('SERVICE') && hasAuthority('accounts.write')")
    public String cash(@RequestBody CashRequest request) {
        log.debug("Authorities in CASH: {}", request);
        accountService.cash(request);

        return "Операция выполнена: "
            + request.getAction()
            + " " + request.getValue();
    }

    @PostMapping("/transfer")
    @PreAuthorize("hasRole('SERVICE') && hasAuthority('accounts.write')")
    public String transfer(@RequestBody TransferRequest request) {
        log.debug("Authorities in TRANSFER: {}", request);
        accountService.transfer(request);
        return "Перевод выполнен: "
            + request.getAmount()
            + " со счёта " + request.getFromLogin()
            + " на счёт " + request.getToLogin();
    }

    @GetMapping("{accountLogin}/owner")
    @PreAuthorize("hasRole('SERVICE')")
    public AccountOwnerResponse getOwner(@PathVariable String accountLogin) {
        var account = accountService.getAccountByLogin(accountLogin);
        return new AccountOwnerResponse(account.getKeycloakId().toString(), account.getLogin());
    }

}
