package yandex.workshop.accountsservice.repository;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import yandex.workshop.accountsservice.entity.Account;

public interface AccountRepository extends JpaRepository<Account, Long> {
    Optional<Account> findByLogin(String login);

    @Query("SELECT a FROM Account a WHERE a.keycloakId = ?1")
    Optional<Account> findByKeycloakId(UUID keycloakId);
}

