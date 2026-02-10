package yandex.workshop.accountsservice;

import java.math.BigDecimal;
import java.time.LocalDate;
import yandex.workshop.account.api.accounts.model.AccountResponse;
import yandex.workshop.accountsservice.entity.Account;

public class TestData {

    public static AccountResponse accountResponse() {
        return new AccountResponse(
            "ivanov123",
            "Иванов Иван",
            LocalDate.of(1990, 1, 1),
            BigDecimal.valueOf(1000)
        );
    }


    public static Account account() {
        Account account = new Account();
        account.setKeycloakId(java.util.UUID.randomUUID());
        account.setLogin("ivanov123");
        account.setFirstName("Иван");
        account.setLastName("Иванов");
        account.setBirthDate(LocalDate.of(1990, 1, 1));
        account.setBalance(BigDecimal.valueOf(1000));
        return account;
    }


}
