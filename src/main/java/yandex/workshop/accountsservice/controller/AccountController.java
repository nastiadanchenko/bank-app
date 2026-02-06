package yandex.workshop.accountsservice.controller;

import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
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

    @GetMapping(value = "/me",
        produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasRole('SERVICE')")
    public ResponseEntity<AccountResponse> getOwnerAccount(Authentication authentication) {
        return ResponseEntity.ok(accountService.getCurrentAccount(authentication));
    }

    @PostMapping(value = "/me",
        produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasRole('SERVICE') && hasAuthority('accounts.write')")
    public AccountResponse updateAccount(Authentication authentication,
                                         @RequestBody UpdateAccountRequest dto) {

//        log.debug("Authorities in PUT: {}", token.getAuthorities());
        return accountService.updateProfile(authentication, dto);
    }


    @GetMapping(value = "",
        produces = MediaType.APPLICATION_JSON_VALUE)
    public List<AccountResponse> getAccounts(JwtAuthenticationToken token) {
        return accountService.getOtherAccounts(token.getName());
    }

    @PostMapping(value = "/cash")
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

    @GetMapping(value = "{accountLogin}/owner",
        produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasRole('SERVICE')")
    public AccountOwnerResponse getOwner(@PathVariable String accountLogin) {
        var account = accountService.getAccountByLogin(accountLogin);
        return new AccountOwnerResponse(account.getKeycloakId().toString(), account.getLogin());
    }

}
