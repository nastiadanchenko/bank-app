package yandex.workshop.accountsservice.service;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import yandex.workshop.accountsservice.entity.Account;
import yandex.workshop.accountsservice.repository.AccountRepository;
import yandex.workshop.api.model.AccountResponse;
import yandex.workshop.api.model.CashRequest;
import yandex.workshop.api.model.NotificationRequest;
import yandex.workshop.api.model.TransferRequest;
import yandex.workshop.api.model.UpdateAccountRequest;
import yandex.workshop.sharedkafka.NotificationProducer;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class AccountService {

    @Value("${topic.notification}")
    private String topicName;

    @Value("${spring.application.name}")
    private String serviceName;

    private final AccountRepository accountRepository;

    private final NotificationProducer notificationProducer;

    public AccountResponse getCurrentAccount(Authentication authentication) {
        log.debug("Request to get current account");

        Jwt jwt = (Jwt) authentication.getPrincipal();
        UUID keycloakId = UUID.fromString(jwt.getSubject());
        Account account = accountRepository.findByKeycloakId(keycloakId)
            .orElseGet(() -> {
                log.info("Account not found. Creating new account for keycloakId={}", keycloakId);

                Account newAccount = new Account();
                newAccount.setKeycloakId(keycloakId);
                newAccount.setFirstName(jwt.getClaimAsString("given_name"));
                newAccount.setLastName(jwt.getClaimAsString("family_name"));
                newAccount.setLogin(jwt.getClaimAsString("preferred_username"));
                newAccount.setBalance(BigDecimal.ZERO);
                newAccount.setBirthDate(LocalDate.now());
                return accountRepository.save(newAccount);
            });

        log.info("Account retrieved login={}, balance={}", account.getLogin(), account.getBalance());
        return buildDto(account);
    }

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

        return buildDto(account);
    }

    public AccountResponse updateProfile(Authentication authentication, UpdateAccountRequest dto) {
        Jwt jwt = (Jwt) authentication.getPrincipal();
        UUID keycloakId = UUID.fromString(jwt.getSubject());
        String[] nams = dto.getName().split(" ", 2);

        log.info("Update profile request for keycloakId={}", keycloakId);

        Account account = accountRepository.findByKeycloakId(keycloakId)
            .orElseThrow(() -> {
                log.error("Account not found for keycloakId={}", keycloakId);
                return new NoSuchElementException("Not found account with keycloakId " + keycloakId);
            });
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

        sendNotification("Account updated for user: " + account.getLogin(),
            account.getKeycloakId().toString(), account.getLogin());
        log.info("Account profile updated login={}, birthDate={}",
            account.getLogin(),
            account.getBirthDate());
        return buildDto(accountRepository.save(account));

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

        sendNotification("Account updated for user: " + account.getLogin(),
            account.getKeycloakId().toString(), account.getLogin());

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

        log.info("Transfer started from={} to={} amount={}",
            request.getFromLogin(),
            request.getToLogin(),
            request.getAmount());

        if (fromAccount.getBalance().compareTo(request.getAmount()) < 0) {
            log.warn("Transfer rejected due to insufficient funds from={} balance={} requested={}",
                fromAccount.getLogin(),
                fromAccount.getBalance(),
                request.getAmount());
            throw new IllegalArgumentException("Недостаточно средств на счёте " + fromAccount.getId());
        }

        fromAccount.setBalance(fromAccount.getBalance().subtract(request.getAmount()));
        toAccount.setBalance(toAccount.getBalance().add(request.getAmount()));

        accountRepository.save(fromAccount);
        accountRepository.save(toAccount);

        log.info("Transfer completed from={} to={} amount={}",
            fromAccount.getLogin(),
            toAccount.getLogin(),
            request.getAmount());
    }


    public Account getAccountByLogin(String login) {
        return accountRepository.findByLogin(login)
            .orElseThrow(() -> new NoSuchElementException("Not found account with login " + login));
    }

    public void cash(CashRequest request) {
        String sub = request.getAccountId();
        Account account = accountRepository.findByKeycloakId(UUID.fromString(sub))
            .orElseThrow(() -> new NoSuchElementException("Not found account with keycloakId " + sub));

        log.info("Cash operation action={} account={} amount={}",
            request.getAction(),
            account.getLogin(),
            request.getValue());

        if (request.getAction().equals("PUT")) {
            account.setBalance(account.getBalance().add(request.getValue()));
        } else if (request.getAction().equals("GET")) {
            if (account.getBalance().compareTo(request.getValue()) < 0) {
                log.warn("Cash withdraw rejected account={} balance={} requested={}",
                    account.getLogin(),
                    account.getBalance(),
                    request.getValue());
                throw new IllegalArgumentException("Недостаточно средств на счёте " + account.getId());

            }
            account.setBalance(account.getBalance().subtract(request.getValue()));
        } else {
            throw new IllegalArgumentException("Unknown cash action: " + request.getAction());
        }

        log.info("Cash operation completed action={} account={} newBalance={}",
            request.getAction(),
            account.getLogin(),
            account.getBalance());

        accountRepository.save(account);
    }

    private void sendNotification(String message, String userId, String login) {
        log.info("Sending notification service={} user={} login={} topic={}",
            serviceName,
            userId,
            login,
            topicName);

        notificationProducer.send(
            new NotificationRequest()
                .serviceName(serviceName)
                .message(message)
                .timestamp(Instant.now())
                .userId(userId)
                .login(login),
            topicName);

    }
}

