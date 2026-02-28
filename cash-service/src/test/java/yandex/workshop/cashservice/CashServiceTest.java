package yandex.workshop.cashservice;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.test.util.ReflectionTestUtils;
import yandex.workshop.api.model.CashRequest;
import yandex.workshop.api.model.NotificationRequest;
import yandex.workshop.cashservice.client.AccountsClient;
import yandex.workshop.cashservice.service.CashService;
import yandex.workshop.sharedkafka.NotificationProducer;

@ExtendWith(MockitoExtension.class)
public class CashServiceTest {
    @Mock
    private AccountsClient accountsClient;

    @Mock
    private NotificationProducer notificationProducer;

    private CashService cashService;

    @Value("${topic.notification}")
    public String testTopicName;

    @BeforeEach
    void setUp() {
        cashService = new CashService(accountsClient, notificationProducer);
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
        verify(notificationProducer).send(cap.capture(), eq(testTopicName));

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
        verify(notificationProducer).send(cap.capture(), eq(testTopicName));

        NotificationRequest sent = cap.getValue();
        assertThat(sent.getServiceName()).isEqualTo("cash-service");
        assertThat(sent.getMessage()).contains("Cash operation")
            .contains("GET")
            .contains("5.00")
            .contains("bob");
    }
}
