package yandex.workshop.cashservice;

import static org.mockito.Mockito.when;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import java.math.BigDecimal;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import yandex.workshop.cashservice.api.accounts.model.CashRequest;
import yandex.workshop.cashservice.api.accounts.model.NotificationRequest;
import yandex.workshop.cashservice.client.AccountsClient;
import yandex.workshop.cashservice.client.NotificationClient;
import yandex.workshop.cashservice.service.CashService;
@ExtendWith(MockitoExtension.class)
public class CashServiceTest {
    @Mock
    private AccountsClient accountsClient;

    @Mock
    private NotificationClient notificationClient;

    private CashService cashService;

    @BeforeEach
    void setUp() {
        cashService = new CashService(accountsClient, notificationClient);
        ReflectionTestUtils.setField(cashService, "serviceName", "cash-service");
    }

    @Test
    void submit_callsAccountsClientAndSendsNotification_onSuccess() {
        CashRequest req = new CashRequest();
        req.setAction("PUT");
        req.setValue(new BigDecimal("10.00"));
        req.setAccountLogin("alice");

        when(accountsClient.sendTransaction(any(CashRequest.class))).thenReturn("OK: added 10.00");

        String result = cashService.submit(req);

        assertThat(result).isEqualTo("OK: added 10.00");

        ArgumentCaptor<NotificationRequest> cap = ArgumentCaptor.forClass(NotificationRequest.class);
        verify(notificationClient).notify(cap.capture());

        NotificationRequest sent = cap.getValue();
        assertThat(sent).isNotNull();
        assertThat(sent.getServiceName()).isEqualTo("cash-service");
        assertThat(sent.getMessage()).contains("Cash operation")
            .contains("PUT")
            .contains("10.00")
            .contains("alice");
    }

    @Test
    void submit_returnsErrorMessage_whenAccountsClientFails_andStillSendsNotification() {
        CashRequest req = new CashRequest();
        req.setAction("GET");
        req.setValue(new BigDecimal("5.00"));
        req.setAccountLogin("bob");

        String errorMsg = "Ошибка при обращении к accounts-service: timeout";
        when(accountsClient.sendTransaction(any(CashRequest.class))).thenReturn(errorMsg);

        String result = cashService.submit(req);

        assertThat(result).isEqualTo(errorMsg);

        ArgumentCaptor<NotificationRequest> cap = ArgumentCaptor.forClass(NotificationRequest.class);
        verify(notificationClient).notify(cap.capture());

        NotificationRequest sent = cap.getValue();
        assertThat(sent.getServiceName()).isEqualTo("cash-service");
        assertThat(sent.getMessage()).contains("Cash operation")
            .contains("GET")
            .contains("5.00")
            .contains("bob");
    }
}
