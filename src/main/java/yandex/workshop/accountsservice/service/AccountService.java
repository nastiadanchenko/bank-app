package yandex.workshop.accountsservice.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import yandex.workshop.account.api.accounts.model.AccountResponse;
import yandex.workshop.account.api.accounts.model.CashRequest;
import yandex.workshop.account.api.accounts.model.NotificationRequest;
import yandex.workshop.account.api.accounts.model.TransferRequest;
import yandex.workshop.account.api.accounts.model.UpdateAccountRequest;
import yandex.workshop.accountsservice.client.NotificationClient;
import yandex.workshop.accountsservice.entity.Account;
import yandex.workshop.accountsservice.repository.AccountRepository;

@Service
@Transactional
@RequiredArgsConstructor
public class AccountService {

    @Value("${spring.application.name}")
    private String serviceName;

    private final AccountRepository accountRepository;

    private final NotificationClient notificationClient;

    public AccountResponse getCurrentAccount(JwtAuthenticationToken token) {
        UUID keycloakId = UUID.fromString(token.getToken().getSubject());
        Account account = accountRepository.findByKeycloakId(keycloakId)
            .orElseGet(() -> {
                Account newAccount = new Account();
                newAccount.setKeycloakId(keycloakId);
                newAccount.setFirstName(token.getToken().getClaimAsString("given_name"));
                newAccount.setLastName(token.getToken().getClaimAsString("family_name"));
                newAccount.setLogin(token.getToken().getClaimAsString("preferred_username"));
                newAccount.setBalance(BigDecimal.ZERO);
                newAccount.setBirthDate(LocalDate.now());
                return accountRepository.save(newAccount);
            });

//        sendNotification("Account accessed for user: " +  account.getLogin());

        return buildDto(account);
    }

    public AccountResponse updateProfile(String keycloakId, UpdateAccountRequest dto) {
        String[] nams = dto.getName().split(" ", 2);
        Account account = accountRepository.findByKeycloakId(UUID.fromString(keycloakId))
            .orElseThrow(() -> new NoSuchElementException("Not found account with keycloakId " + keycloakId));
        if (nams.length != 0) {
            if (!nams[0].isBlank()) {
                account.setFirstName(nams[0]);
            }
            if (nams.length > 1 && !nams[1].isBlank()) {
                account.setLastName(nams[1]);
            }
        }
        if (dto.getBirthdate() != null) {
            account.setBirthDate(dto.getBirthdate());
        }

        sendNotification("Account updated for user: " +  account.getLogin());

        return buildDto(accountRepository.save(account));

    }


    public List<AccountResponse> getOtherAccounts(String login) {
        return accountRepository.findAll().stream()
            .filter(account -> !account.getKeycloakId().equals(UUID.fromString(login)))
            .map(this::buildDto)
            .toList();
    }


    public Account getAccountById(Long id) {
        return accountRepository.findById(id)
            .orElseThrow(() -> new NoSuchElementException("Not found account with id " + id));
    }

    private AccountResponse buildDto(Account account) {
        return new AccountResponse()
            .login(account.getLogin())
            .name(account.getFirstName() + " " + account.getLastName())
            .birthdate(account.getBirthDate())
            .balance(account.getBalance());
    }

    @Transactional
    public void transfer(TransferRequest request) {
        Account fromAccount = getAccountByLogin(request.getFromLogin());
        Account toAccount = getAccountByLogin(request.getToLogin());

        if (fromAccount.getBalance().compareTo(request.getAmount()) < 0) {
            throw new IllegalArgumentException("Недостаточно средств на счёте " + fromAccount.getId());
        }

        fromAccount.setBalance(fromAccount.getBalance().subtract(request.getAmount()));
        toAccount.setBalance(toAccount.getBalance().add(request.getAmount()));

        accountRepository.save(fromAccount);
        accountRepository.save(toAccount);
    }


    public Account getAccountByLogin(String login) {
        return accountRepository.findByLogin(login)
            .orElseThrow(() -> new NoSuchElementException("Not found account with login " + login));
    }

    public void cash(CashRequest request) {
        String sub = request.getAccountId();
        Account account = accountRepository.findByKeycloakId(UUID.fromString(sub))
            .orElseThrow(() -> new NoSuchElementException("Not found account with keycloakId " + sub));


        if (request.getAction().equals("PUT")) {
            account.setBalance(account.getBalance().add(request.getValue()));
        } else if (request.getAction().equals("GET")) {
            if (account.getBalance().compareTo(request.getValue()) < 0) {
                throw new IllegalArgumentException("Недостаточно средств на счёте " + account.getId());

            }
            account.setBalance(account.getBalance().subtract(request.getValue()));
        } else {
            throw new IllegalArgumentException("Unknown cash action: " + request.getAction());
        }

        accountRepository.save(account);
    }

    private void sendNotification(String message) {
        notificationClient.notify(
            new NotificationRequest()
                .serviceName(serviceName)
                .message(message));
    }
}

