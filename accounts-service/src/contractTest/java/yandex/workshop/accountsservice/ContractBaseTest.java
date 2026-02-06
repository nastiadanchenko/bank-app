package yandex.workshop.accountsservice;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.beans.factory.annotation.Autowired;
import yandex.workshop.accountsservice.controller.AccountController;
import yandex.workshop.accountsservice.service.AccountService;
import io.restassured.module.mockmvc.RestAssuredMockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@AutoConfigureMockMvc()
@ActiveProfiles("contract-test")
@WebMvcTest(value = AccountController.class,
    properties = "spring.cloud.config.enabled=false")
@WithMockUser(roles = "SERVICE",
     username = "contract-test", password = "password")
@Import({SecurityConfig.class})
public abstract class ContractBaseTest {

    @Autowired
    protected MockMvc mockMvc;

    @MockitoBean
    protected AccountService accountService;

    @BeforeEach
    void setup() {
        RestAssuredMockMvc.mockMvc(mockMvc);

        when(accountService.getCurrentAccount(any(UsernamePasswordAuthenticationToken.class)))
            .thenReturn(TestData.accountResponse());

        when(accountService.updateProfile(any(UsernamePasswordAuthenticationToken.class), any()))
            .thenReturn(TestData.accountResponse());

        when(accountService.getAccountByLogin(any()))
            .thenReturn(TestData.account());
    }
}