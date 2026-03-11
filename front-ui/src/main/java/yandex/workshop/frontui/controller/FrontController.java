package yandex.workshop.frontui.controller;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import yandex.workshop.api.model.CashRequest;
import yandex.workshop.api.model.OperationResponse;
import yandex.workshop.api.model.TransferRequest;
import yandex.workshop.api.model.UpdateAccountRequest;
import yandex.workshop.frontui.client.AccountsClient;
import yandex.workshop.frontui.client.CashClient;
import yandex.workshop.frontui.client.TransferClient;

@Slf4j
@Controller
@RequiredArgsConstructor
public class FrontController {

    private final AccountsClient accountsClient;
    private final CashClient cashClient;
    private final TransferClient transferClient;

    @GetMapping("/")
    public String index(Model model) {
        log.debug("Loading index page");

        var account = accountsClient.getMyAccount();

        log.info("Account page opened login={} balance={}",
            account.login(),
            account.balance());
        model.addAttribute("name", account.name());
        model.addAttribute("birthdate", account.birthdate());
        model.addAttribute("sum", account.balance());
        model.addAttribute("accounts", accountsClient.getOtherAccounts());

        return "index";
    }

    @PostMapping("/account")
    public String updateAccount(
        @RequestParam String name,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate birthdate
    ) {
        log.info("Account update requested name={} birthdate={}", name, birthdate);

        var req = new UpdateAccountRequest()
            .name(name)
            .birthdate(birthdate);

        accountsClient.updateAccount(req);
        log.info("Account update completed name={}", name);
        return "redirect:/";
    }

    @PostMapping("/cash")
    public String cash(
        @RequestParam BigDecimal value,
        @RequestParam String action,
        RedirectAttributes redirectAttributes,
        OAuth2AuthenticationToken token
    ) {
        String login = token.getPrincipal().getAttribute("preferred_username");
        log.info("Cash request action={} login={} amount={}",
            action,
            login,
            value);

        var req = new CashRequest()
            .value(value)
            .action(action)
            .accountId(token.getName())
            .accountLogin(login);

        OperationResponse resp = cashClient.cash(req);
        if (Boolean.TRUE.equals(resp.getSuccess())) {
            log.info("Cash operation successful login={} action={} amount={} message={}",
                login,
                action,
                value,
                resp.getMessage());
            redirectAttributes.addFlashAttribute("info", resp.getMessage());
        } else {
            log.warn("Cash operation failed login={} action={} amount={} reason={}",
                login,
                action,
                value,
                resp.getMessage());
            redirectAttributes.addFlashAttribute("errors", List.of(resp.getMessage()));
        }
        return "redirect:/";
    }

    @PostMapping("/transfer")
    public String transfer(
        @RequestParam String login,
        @RequestParam BigDecimal value,
        RedirectAttributes redirectAttributes,
        OAuth2AuthenticationToken token
    ) {
        String fromLogin = token.getPrincipal().getAttribute("preferred_username");

        log.info("Transfer request from={} to={} amount={}",
            fromLogin,
            login,
            value);

        var req = new TransferRequest()
            .fromLogin(fromLogin)
            .toLogin(login)
            .amount(value);

        OperationResponse resp = transferClient.transfer(req);
        if (Boolean.TRUE.equals(resp.getSuccess())) {
            log.info("Transfer request from={} to={} amount={}",
                fromLogin,
                login,
                value);
            redirectAttributes.addFlashAttribute("info", resp.getMessage());
        } else {
            log.warn("Transfer failed from={} to={} amount={} reason={}",
                fromLogin,
                login,
                value,
                resp.getMessage());
            redirectAttributes.addFlashAttribute("errors", List.of(resp.getMessage()));
        }

        return "redirect:/";
    }
}
