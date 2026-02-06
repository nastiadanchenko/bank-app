package yandex.workshop.frontui;

import static org.mockito.ArgumentMatchers.any;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import yandex.workshop.frontui.api.accounts.model.CashRequest;
import yandex.workshop.frontui.api.accounts.model.OperationResponse;
import yandex.workshop.frontui.api.accounts.model.TransferRequest;
import yandex.workshop.frontui.api.accounts.model.UpdateAccountRequest;
import yandex.workshop.frontui.client.AccountsClient;
import yandex.workshop.frontui.client.CashClient;
import yandex.workshop.frontui.client.TransferClient;
import yandex.workshop.frontui.controller.FrontController;
import yandex.workshop.frontui.dto.AccountDto;

@WebMvcTest(FrontController.class)
@ActiveProfiles("test")
@AutoConfigureMockMvc(addFilters = false)
@TestPropertySource(properties = "spring.cloud.config.enabled=false")
class FrontControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AccountsClient accountsClient;

    @MockitoBean
    private CashClient cashClient;

    @MockitoBean
    private TransferClient transferClient;

    @Test
    void index_populatesModelAndReturnsIndexView() throws Exception {
        AccountDto me = new AccountDto("meuser", "Me User", LocalDate.of(1990, 1, 1), new BigDecimal("123.45"));
        List<AccountDto> others = List.of(
            new AccountDto("a", "A A", LocalDate.of(1991, 2, 2), new BigDecimal("10")),
            new AccountDto("b", "B B", LocalDate.of(1992, 3, 3), new BigDecimal("20"))
        );

        when(accountsClient.getMyAccount()).thenReturn(me);
        when(accountsClient.getOtherAccounts()).thenReturn(others);

        mockMvc.perform(get("/"))
            .andExpect(status().isOk())
            .andExpect(view().name("index"))
            .andExpect(model().attribute("name", "Me User"))
            .andExpect(model().attribute("birthdate", LocalDate.of(1990, 1, 1)))
            .andExpect(model().attribute("sum", new BigDecimal("123.45")))
            .andExpect(model().attribute("accounts", others));
    }

    @Test
    void updateAccount_callsClientAndRedirects() throws Exception {
        String name = "Иванов Иван";
        String birthdate = "1990-01-01";

        mockMvc.perform(post("/account")
                .param("name", name)
                .param("birthdate", birthdate)
                .with(request -> {
                    var attributes = Map.of(
                        "preferred_username", (Object) "meuser",
                        "sub", (Object) UUID.randomUUID().toString()
                    );
                    var principal = new DefaultOAuth2User(
                        List.of(new SimpleGrantedAuthority("ROLE_USER")),
                        attributes,
                        "sub"
                    );
                    request.setUserPrincipal(new OAuth2AuthenticationToken(principal, principal.getAuthorities(), "client"));
                    return request;
                }))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/"));

        ArgumentCaptor<UpdateAccountRequest> cap = ArgumentCaptor.forClass(UpdateAccountRequest.class);
        verify(accountsClient).updateAccount(cap.capture());
        UpdateAccountRequest sent = cap.getValue();

        assert sent.getName().equals(name);
        assert sent.getBirthdate().equals(LocalDate.parse(birthdate));
    }

    @Test
    void cash_postsToCashClient_andAddsFlashInfoOnSuccess() throws Exception {
        UUID sub = UUID.randomUUID();
        String preferredUsername = "alice";

        OperationResponse okResp = new OperationResponse();
        okResp.setSuccess(true);
        okResp.setMessage("Пополнение выполнено");

        when(cashClient.cash(any())).thenReturn(okResp);

        var attributes = Map.<String, Object>of(
            "sub", sub.toString(),
            "preferred_username", preferredUsername
        );
        var principal = new DefaultOAuth2User(
            List.of(new SimpleGrantedAuthority("ROLE_USER")),
            attributes,
            "sub"
        );
        OAuth2AuthenticationToken token = new OAuth2AuthenticationToken(principal, principal.getAuthorities(), "client");

        mockMvc.perform(post("/cash")
                .param("value", "50.00")
                .param("action", "PUT")
                .with(request -> {
                    request.setUserPrincipal(token);
                    return request;
                }))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/"))
            .andExpect(flash().attribute("info", "Пополнение выполнено"));

        ArgumentCaptor<CashRequest> cap = ArgumentCaptor.forClass(CashRequest.class);
        verify(cashClient).cash(cap.capture());
        CashRequest sent = cap.getValue();

        assert sent.getAccountId().equals(sub.toString());
        assert sent.getAccountLogin().equals(preferredUsername);
        assert sent.getAction().equals("PUT");
        assert sent.getValue().compareTo(new BigDecimal("50.00")) == 0;
    }

    @Test
    void transfer_postsToTransferClient_andAddsFlashInfoOnSuccess() throws Exception {
        String preferredUsername = "fromUser";
        var attributes = Map.<String, Object>of("preferred_username", preferredUsername);
        var principal = new DefaultOAuth2User(
            List.of(new SimpleGrantedAuthority("ROLE_USER")),
            attributes,
            "preferred_username"
        );
        OAuth2AuthenticationToken token = new OAuth2AuthenticationToken(principal, principal.getAuthorities(), "client");

        OperationResponse okResp = new OperationResponse();
        okResp.setSuccess(true);
        okResp.setMessage("Перевод выполнен");
        when(transferClient.transfer(any())).thenReturn(okResp);

        mockMvc.perform(post("/transfer")
                .param("login", "toUser")
                .param("value", "15.50")
                .with(request -> {
                    request.setUserPrincipal(token);
                    return request;
                }))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/"))
            .andExpect(flash().attribute("info", "Перевод выполнен"));

        ArgumentCaptor<TransferRequest> cap = ArgumentCaptor.forClass(TransferRequest.class);
        verify(transferClient).transfer(cap.capture());
        TransferRequest sent = cap.getValue();
        assert sent.getFromLogin().equals(preferredUsername);
        assert sent.getToLogin().equals("toUser");
        assert sent.getAmount().compareTo(new BigDecimal("15.50")) == 0;
    }
}
