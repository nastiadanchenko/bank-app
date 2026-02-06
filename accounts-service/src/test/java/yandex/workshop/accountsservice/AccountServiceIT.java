package yandex.workshop.accountsservice;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import yandex.workshop.account.api.accounts.model.CashRequest;
import yandex.workshop.account.api.accounts.model.NotificationRequest;
import yandex.workshop.account.api.accounts.model.TransferRequest;
import yandex.workshop.account.api.accounts.model.UpdateAccountRequest;
import yandex.workshop.accountsservice.client.NotificationClient;
import yandex.workshop.accountsservice.entity.Account;
import yandex.workshop.accountsservice.repository.AccountRepository;
import yandex.workshop.accountsservice.service.AccountService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

@SpringBootTest(properties = {
    "spring.cloud.config.enabled=false",
    "spring.liquibase.enabled=true",
    "spring.jpa.hibernate.ddl-auto=none"
})
@Import(TestcontainersConfiguration.class)
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
public class AccountServiceIT {
    @Autowired
    private AccountService accountService;

    @Autowired
    private AccountRepository accountRepository;

    @MockitoBean
    private NotificationClient notificationClient;

    @BeforeEach
    void cleanup() {
        accountRepository.deleteAll();
    }

    @Test
    void getCurrentAccount_createsNewAccountWhenNotExists() {
        UUID keycloakId = UUID.randomUUID();
        Jwt jwt = new Jwt(
            "fake-token",
            Instant.now(),
            Instant.now().plusSeconds(3600),
            Map.of("alg", "none"),
            Map.of(
                "sub", keycloakId.toString(),
                "given_name", "John",
                "family_name", "Doe",
                "preferred_username", "jdoe"
            )
        );
        JwtAuthenticationToken token = new JwtAuthenticationToken(jwt);

        var resp = accountService.getCurrentAccount(token);

        assertThat(resp).isNotNull();
        assertThat(resp.getLogin()).isEqualTo("jdoe");
        assertThat(resp.getName()).isEqualTo("John Doe");
        assertThat(accountRepository.findByKeycloakId(keycloakId)).isPresent();
    }

    @Test
    void updateProfile_updatesFieldsAndSendsNotification() {
        UUID keycloakId = UUID.randomUUID();
        Account saved = accountRepository.save(new Account(
            null,
            "user1",
            "OldFirst",
            "OldLast",
            LocalDate.of(1990, 1, 1),
            new BigDecimal("10.00"),
            keycloakId
        ));

        UpdateAccountRequest dto = new UpdateAccountRequest();
        dto.setName("NewFirst NewLast");
        dto.setBirthdate(LocalDate.of(1985, 5, 5));

        var response = accountService.updateProfile(keycloakId.toString(), dto);

        assertThat(response).isNotNull();
        assertThat(response.getName()).isEqualTo("NewFirst NewLast");
        var updated = accountRepository.findById(saved.getId()).orElseThrow();
        assertThat(updated.getFirstName()).isEqualTo("NewFirst");
        assertThat(updated.getLastName()).isEqualTo("NewLast");
        assertThat(updated.getBirthDate()).isEqualTo(LocalDate.of(1985, 5, 5));

        ArgumentCaptor<NotificationRequest> cap = ArgumentCaptor.forClass(NotificationRequest.class);
        verify(notificationClient).notify(cap.capture());
        assertThat(cap.getValue().getMessage()).contains("Account updated for user");
    }

    @Test
    void transfer_movesMoneyBetweenAccounts() {
        Account a = accountRepository.save(new Account(
            null,
            "alice",
            "Alice",
            "A",
            LocalDate.of(1990, 1, 1),
            new BigDecimal("100.00"),
            UUID.randomUUID()
        ));
        Account b = accountRepository.save(new Account(
            null,
            "bob",
            "Bob",
            "B",
            LocalDate.of(1990, 1, 1),
            new BigDecimal("10.00"),
            UUID.randomUUID()
        ));

        TransferRequest req = new TransferRequest();
        req.setFromLogin("alice");
        req.setToLogin("bob");
        req.setAmount(new BigDecimal("25.00"));

        accountService.transfer(req);

        var aAfter = accountRepository.findById(a.getId()).orElseThrow();
        var bAfter = accountRepository.findById(b.getId()).orElseThrow();

        assertThat(aAfter.getBalance()).isEqualByComparingTo(new BigDecimal("75.00"));
        assertThat(bAfter.getBalance()).isEqualByComparingTo(new BigDecimal("35.00"));
    }

    @Test
    void cash_putAndGetBehaveCorrectly() {
        UUID keycloakId = UUID.randomUUID();
        Account account = accountRepository.save(new Account(
            null,
            "cashuser",
            "Cash",
            "User",
            LocalDate.of(1990, 1, 1),
            new BigDecimal("50.00"),
            keycloakId
        ));

        // PUT (add funds)
        CashRequest putReq = new CashRequest();
        putReq.setAccountId(keycloakId.toString());
        putReq.setAction("PUT");
        putReq.setValue(new BigDecimal("20.00"));
        accountService.cash(putReq);

        var accAfterPut = accountRepository.findById(account.getId()).orElseThrow();
        assertThat(accAfterPut.getBalance()).isEqualByComparingTo(new BigDecimal("70.00"));

        // GET (withdraw funds)
        CashRequest getReq = new CashRequest();
        getReq.setAccountId(keycloakId.toString());
        getReq.setAction("GET");
        getReq.setValue(new BigDecimal("30.00"));
        accountService.cash(getReq);

        var accAfterGet = accountRepository.findById(account.getId()).orElseThrow();
        assertThat(accAfterGet.getBalance()).isEqualByComparingTo(new BigDecimal("40.00"));
    }
}
