package yandex.workshop.accountsservice.controller;

import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.openapitools.api.ApiApi;
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
import yandex.workshop.accountsservice.service.AccountService;
import yandex.workshop.api.model.AccountOwnerResponse;
import yandex.workshop.api.model.AccountResponse;
import yandex.workshop.api.model.CashRequest;
import yandex.workshop.api.model.OperationResponse;
import yandex.workshop.api.model.TransferRequest;
import yandex.workshop.api.model.UpdateAccountRequest;

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

        return accountService.updateProfile(authentication, dto);
    }


    @GetMapping(value = "",
        produces = MediaType.APPLICATION_JSON_VALUE)
    public List<AccountResponse> getAccounts(JwtAuthenticationToken token) {
        return accountService.getOtherAccounts(token.getName());
    }

    @PostMapping(value = "/cash")
    @PreAuthorize("hasRole('SERVICE') && hasAuthority('accounts.write')")
    public OperationResponse cash(@RequestBody CashRequest request) {
        log.debug("Cash request: {}", request);

        accountService.cash(request);

        return new OperationResponse()
            .success(true)
            .message(String.format("Операция %s выполнена: %s",
                request.getAction(), request.getValue()));
    }

    @PostMapping("/transfer")
    @PreAuthorize("hasRole('SERVICE') && hasAuthority('accounts.write')")
    public OperationResponse transfer(@RequestBody TransferRequest request) {
        log.debug("Transfer request: {}", request);

        accountService.transfer(request);

        return new OperationResponse()
            .success(true)
            .message(String.format("Перевод выполнен: %s  со счёта %s  на счёт %s ",
            request.getAmount() ,request.getFromLogin(),request.getToLogin()));
    }

    @GetMapping(value = "{accountLogin}/owner",
        produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasRole('SERVICE')")
    public AccountOwnerResponse getOwner(@PathVariable String accountLogin) {
        var account = accountService.getAccountByLogin(accountLogin);
        return new AccountOwnerResponse(account.getKeycloakId().toString(), account.getLogin());
    }

}
