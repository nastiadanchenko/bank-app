//package yandex.workshop.accountsservice;
//
//import com.fasterxml.jackson.databind.ObjectMapper;
//import java.math.BigDecimal;
//import java.time.LocalDate;
//import java.util.List;
//import java.util.Map;
//import java.util.UUID;
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.Test;
//import org.mockito.ArgumentCaptor;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
//import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
//import org.springframework.boot.test.context.SpringBootTest;
//import org.springframework.context.annotation.Import;
//import org.springframework.http.MediaType;
//import org.springframework.test.context.ActiveProfiles;
//import org.springframework.test.context.bean.override.mockito.MockitoBean;
//import org.springframework.test.web.servlet.MockMvc;
//import yandex.workshop.account.api.accounts.model.CashRequest;
//import yandex.workshop.account.api.accounts.model.NotificationRequest;
//import yandex.workshop.account.api.accounts.model.TransferRequest;
//import yandex.workshop.account.api.accounts.model.UpdateAccountRequest;
//import yandex.workshop.accountsservice.client.NotificationClient;
//import yandex.workshop.accountsservice.entity.Account;
//import yandex.workshop.accountsservice.repository.AccountRepository;
//import static org.mockito.Mockito.verify;
//import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
//import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
//import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
//import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
//import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
//
//
//@SpringBootTest(properties = {
//    "spring.cloud.config.enabled=false",
//    "spring.liquibase.enabled=true",
//    "spring.jpa.hibernate.ddl-auto=none"
//})
//@Import(TestcontainersConfiguration.class)
//@ActiveProfiles("test")
//@AutoConfigureMockMvc
//@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
//class AccountControllerIT {
//
//    @Autowired
//    private MockMvc mockMvc;
//
//    @Autowired
//    private AccountRepository accountRepository;
//
//    @MockitoBean
//    private NotificationClient notificationClient;
//
//    @Autowired
//    private ObjectMapper objectMapper;
//
//    @BeforeEach
//    void init() {
//        accountRepository.deleteAll();
//    }
//
//    @Test
//    void getMe_createsAccountWhenMissing_andReturnsAccountResponse() throws Exception {
//        UUID keycloakId = UUID.randomUUID();
//
//        var jwtCustomizer = (org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken) null;
//        mockMvc.perform(get("/accounts/me")
//                .with(jwt().jwt(jwt -> {
//                    jwt.claim("sub", keycloakId.toString());
//                    jwt.claim("given_name", "John");
//                    jwt.claim("family_name", "Doe");
//                    jwt.claim("preferred_username", "jdoe");
//                    jwt.claim("realm_access", Map.of("roles", List.of("SERVICE")));
//                })))
//            .andExpect(status().isOk())
//            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
//            .andExpect(jsonPath("$.login").value("jdoe"))
//            .andExpect(jsonPath("$.name").value("John Doe"))
//            .andExpect(jsonPath("$.balance").isNumber());
//
//        assert(accountRepository.findByKeycloakId(keycloakId).isPresent());
//    }
//
//    @Test
//    void updateMe_updatesProfile_andSendsNotification() throws Exception {
//        UUID keycloakId = UUID.randomUUID();
//        accountRepository.save(new Account(
//            null,
//            "user1",
//            "OldFirst",
//            "OldLast",
//            LocalDate.of(1990, 1, 1),
//            new BigDecimal("10.00"),
//            keycloakId
//        ));
//
//        UpdateAccountRequest dto = new UpdateAccountRequest();
//        dto.setName("NewFirst NewLast");
//        dto.setBirthdate(LocalDate.of(1985, 5, 5));
//
//        mockMvc.perform(post("/accounts/me")
//                .with(jwt().jwt(jwt -> {
//                    jwt.claim("sub", keycloakId.toString());
//                    jwt.claim("realm_access", Map.of("roles", List.of("SERVICE", "ACCOUNTS_WRITE"))); // ROLE_SERVICE + accounts.write
//                }))
//                .contentType(MediaType.APPLICATION_JSON)
//                .content(objectMapper.writeValueAsString(dto)))
//            .andExpect(status().isOk())
//            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
//            .andExpect(jsonPath("$.name").value("NewFirst NewLast"));
//
//        var updated = accountRepository.findByKeycloakId(keycloakId).orElseThrow();
//        assert(updated.getFirstName().equals("NewFirst"));
//        assert(updated.getLastName().equals("NewLast"));
//        assert(updated.getBirthDate().equals(LocalDate.of(1985, 5, 5)));
//
//        ArgumentCaptor<NotificationRequest> cap = ArgumentCaptor.forClass(NotificationRequest.class);
//        verify(notificationClient).notify(cap.capture());
//        assert(cap.getValue().getMessage().contains("Account updated for user"));
//    }
//
//    @Test
//    void getAccounts_returnsOtherAccountsExcludingRequester() throws Exception {
//        UUID meId = UUID.randomUUID();
//        accountRepository.save(new Account(null, "meuser", "Me", "User", LocalDate.of(1990,1,1), new BigDecimal("10"), meId));
//        accountRepository.save(new Account(null, "other1", "O1", "User", LocalDate.of(1991,1,1), new BigDecimal("20"), UUID.randomUUID()));
//        accountRepository.save(new Account(null, "other2", "O2", "User", LocalDate.of(1992,1,1), new BigDecimal("30"), UUID.randomUUID()));
//
//        mockMvc.perform(get("/accounts")
//                .with(jwt().jwt(jwt -> {
//                    jwt.claim("sub", meId.toString());
//                    jwt.claim("preferred_username", meId.toString());
//                    jwt.claim("realm_access", Map.of("roles", List.of("SERVICE")));
//                })))
//            .andExpect(status().isOk())
//            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
//            .andExpect(jsonPath("$.length()").value(2))
//            .andExpect(jsonPath("$[?(@.login == 'other1')]").exists())
//            .andExpect(jsonPath("$[?(@.login == 'other2')]").exists());
//    }
//
//    @Test
//    void cash_andTransfer_endpoints_requireAuthorityAndWork() throws Exception {
//        UUID idFrom = UUID.randomUUID();
//        UUID idTo = UUID.randomUUID();
//        accountRepository.save(new Account(null, "alice", "Alice", "A", LocalDate.of(1990,1,1), new BigDecimal("100.00"), idFrom));
//        accountRepository.save(new Account(null, "bob", "Bob", "B", LocalDate.of(1990,1,1), new BigDecimal("10.00"), idTo));
//
//        TransferRequest transferReq = new TransferRequest();
//        transferReq.setFromLogin("alice");
//        transferReq.setToLogin("bob");
//        transferReq.setAmount(new BigDecimal("25.00"));
//
//        mockMvc.perform(post("/accounts/transfer")
//                .with(jwt().jwt(jwt -> {
//                    jwt.claim("realm_access", Map.of("roles", List.of("SERVICE", "ACCOUNTS_WRITE")));
//                }))
//                .contentType(MediaType.APPLICATION_JSON)
//                .content(objectMapper.writeValueAsString(transferReq)))
//            .andExpect(status().isOk())
//            .andExpect(content().string(org.hamcrest.Matchers.containsString("Перевод выполнен")));
//
//        var aAfter = accountRepository.findByLogin("alice").orElseThrow();
//        var bAfter = accountRepository.findByLogin("bob").orElseThrow();
//        assert(aAfter.getBalance().compareTo(new BigDecimal("75.00")) == 0);
//        assert(bAfter.getBalance().compareTo(new BigDecimal("35.00")) == 0);
//
//        CashRequest putReq = new CashRequest();
//        putReq.setAccountId(idFrom.toString());
//        putReq.setAction("PUT");
//        putReq.setValue(new BigDecimal("10.00"));
//
//        mockMvc.perform(post("/accounts/cash")
//                .with(jwt().jwt(jwt -> {
//                    jwt.claim("realm_access", Map.of("roles", List.of("SERVICE", "ACCOUNTS_WRITE")));
//                }))
//                .contentType(MediaType.APPLICATION_JSON)
//                .content(objectMapper.writeValueAsString(putReq)))
//            .andExpect(status().isOk())
//            .andExpect(content().string(org.hamcrest.Matchers.containsString("Операция выполнена")));
//
//        var afterPut = accountRepository.findByKeycloakId(idFrom).orElseThrow();
//        assert(afterPut.getBalance().compareTo(new BigDecimal("85.00")) == 0);
//    }
//}
