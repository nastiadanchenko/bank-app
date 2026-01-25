package yandex.workshop.accountsservice;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import yandex.workshop.account.api.accounts.model.UpdateAccountRequest;

@Service
@Transactional
@RequiredArgsConstructor
public class AccountService {

    private final AccountRepository accountRepository;

    public Account getCurrentAccount(String login) {
        return accountRepository.findByLogin(login)
                .orElseThrow(() -> new RuntimeException("Account not found"));
    }

    public Account updateProfile(String login, UpdateAccountRequest dto) {
        String[] nams = dto.getName().split(" ", 2);
        Account account = getCurrentAccount(login);
        account.setFirstName(nams[0]);
        account.setLastName(nams.length > 1 ? nams[1] : "");
        account.setBirthDate(dto.getBirthdate());
        return accountRepository.save(account);
    }


    public List<Account> getOtherAccounts(String login) {
        return accountRepository.findAll().stream()
                .filter(account -> !account.getLogin().equals(login))
                .toList();
    }
}

