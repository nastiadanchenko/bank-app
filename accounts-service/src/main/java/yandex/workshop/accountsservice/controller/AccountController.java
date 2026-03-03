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
        OperationResponse response;
        log.debug("Authorities in CASH: {}", request);
        try {
            accountService.cash(request);
                response = new OperationResponse()
                    .success(true)
                    .message("Операция выполнена: " + request.getValue());

        } catch (RuntimeException e) {
            log.error("Ошибка при выполнении операции с наличными: {}", e.getMessage());
            response = new OperationResponse()
                .success(false)
                .message("Ошибка при выполнении операции: " + e.getMessage());
        }
        return response;
    }

    @PostMapping("/transfer")
    @PreAuthorize("hasRole('SERVICE') && hasAuthority('accounts.write')")
    public OperationResponse transfer(@RequestBody TransferRequest request) {
        log.debug("Authorities in TRANSFER: {}", request);
        OperationResponse response;
        try {
            accountService.transfer(request);
            response = new OperationResponse()
                .success(true)
                .message("Перевод выполнен: "
                    + request.getAmount()
                    + " со счёта " + request.getFromLogin()
                    + " на счёт " + request.getToLogin());
        } catch (RuntimeException e) {
            log.error("Ошибка при выполнении перевода: {}", e.getMessage());
            response = new OperationResponse()
                .success(false)
                .message("Ошибка при выполнении перевода: " + e.getMessage());
        }

        return response;
    }

    @GetMapping(value = "{accountLogin}/owner",
        produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasRole('SERVICE')")
    public AccountOwnerResponse getOwner(@PathVariable String accountLogin) {
        var account = accountService.getAccountByLogin(accountLogin);
        return new AccountOwnerResponse(account.getKeycloakId().toString(), account.getLogin());
    }

}
