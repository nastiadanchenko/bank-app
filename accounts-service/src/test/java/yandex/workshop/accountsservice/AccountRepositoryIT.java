package yandex.workshop.accountsservice;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import yandex.workshop.accountsservice.entity.Account;
import yandex.workshop.accountsservice.repository.AccountRepository;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
@SpringBootTest(properties = {
    "spring.cloud.config.enabled=false",
    "spring.liquibase.enabled=true",
    "spring.jpa.hibernate.ddl-auto=none"
})
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
public class AccountRepositoryIT {


    @Autowired
    private AccountRepository accountRepository;

    @BeforeEach
    void cleanup() {
        accountRepository.deleteAll();
    }

    @Test
    void saveAndFindByLoginAndKeycloakId() {
        UUID keycloakId = UUID.randomUUID();
        Account account = new Account(
            null,
            "jdoe",
            "John",
            "Doe",
            LocalDate.of(1990, 1, 1),
            new BigDecimal("100.00"),
            keycloakId
        );

        Account saved = accountRepository.save(account);
        assertThat(saved.getId()).isNotNull();

        Optional<Account> byLogin = accountRepository.findByLogin("jdoe");
        assertThat(byLogin).isPresent();
        assertThat(byLogin.get().getKeycloakId()).isEqualTo(keycloakId);

        Optional<Account> byKeycloak = accountRepository.findByKeycloakId(keycloakId);
        assertThat(byKeycloak).isPresent();
        assertThat(byKeycloak.get().getLogin()).isEqualTo("jdoe");
    }
}
