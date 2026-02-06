package yandex.workshop.frontui.controller;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import yandex.workshop.frontui.client.AccountsClient;
import yandex.workshop.frontui.client.CashClient;
import yandex.workshop.frontui.client.TransferClient;
import yandex.workshop.frontui.api.accounts.model.CashRequest;
import yandex.workshop.frontui.api.accounts.model.OperationResponse;
import yandex.workshop.frontui.api.accounts.model.TransferRequest;
import yandex.workshop.frontui.api.accounts.model.UpdateAccountRequest;

@Controller
@RequiredArgsConstructor
public class FrontController {

    private final AccountsClient accountsClient;
    private final CashClient cashClient;
    private final TransferClient transferClient;

    @GetMapping("/")
    public String index(Model model) {
        var account = accountsClient.getMyAccount();

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
        var req = new UpdateAccountRequest()
            .name(name)
            .birthdate(birthdate);

        accountsClient.updateAccount(req);
        return "redirect:/";
    }

    @PostMapping("/cash")
    public String cash(
        @RequestParam BigDecimal value,
        @RequestParam String action,
        RedirectAttributes redirectAttributes,
        OAuth2AuthenticationToken token
    ) {
        var req = new CashRequest()
            .value(value)
            .action(action)
            .accountId(token.getName())
            .accountLogin(token.getPrincipal().getAttribute("preferred_username"));

        OperationResponse resp = cashClient.cash(req);
        if (Boolean.TRUE.equals(resp.getSuccess())) {
            redirectAttributes.addFlashAttribute("info", resp.getMessage());
        } else {
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
        var req = new TransferRequest()
            .fromLogin(fromLogin)
            .toLogin(login)
            .amount(value);

        OperationResponse resp = transferClient.transfer(req);
        if (Boolean.TRUE.equals(resp.getSuccess())) {
            redirectAttributes.addFlashAttribute("info", resp.getMessage());
        } else {
            redirectAttributes.addFlashAttribute("errors", List.of(resp.getMessage()));
        }

        return "redirect:/";
    }
}
