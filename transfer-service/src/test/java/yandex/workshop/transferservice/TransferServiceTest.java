package yandex.workshop.transferservice;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.test.util.ReflectionTestUtils;
import yandex.workshop.transfer_service.api.accounts.model.NotificationRequest;
import yandex.workshop.transfer_service.api.accounts.model.TransferRequest;
import yandex.workshop.transferservice.client.AccountsClient;
import yandex.workshop.transferservice.client.NotificationClient;
import yandex.workshop.transferservice.service.TransferService;

@ExtendWith(MockitoExtension.class)
public class TransferServiceTest {
    @Mock
    private AccountsClient accountsClient;

    @Mock
    private NotificationClient notificationClient;

    private TransferService transferService;

    @BeforeEach
    void setUp() {
        transferService = new TransferService(accountsClient, notificationClient);
        ReflectionTestUtils.setField(transferService, "serviceName", "transfer-service");
    }

    @Test
    void submit_whenOwner_executesTransfer_andSendsNotification() {
        TransferRequest req = new TransferRequest();
        req.setFromLogin("alice");
        req.setToLogin("bob");
        req.setAmount(new BigDecimal("50.00"));

        String username = "aliceUser";

        Jwt jwt = new Jwt(
            "token",
            Instant.now(),
            Instant.now().plusSeconds(3600),
            Map.of("alg", "none"),
            Map.of(
                "preferred_username", username
            )
        );
        JwtAuthenticationToken auth = new JwtAuthenticationToken(jwt);

        when(accountsClient.isOwner("alice", username)).thenReturn(true);
        when(accountsClient.transfer(req)).thenReturn("Перевод выполнен");


        String result = transferService.submit(req, auth);


        assertThat(result).isEqualTo("Перевод выполнен");
        verify(accountsClient).isOwner("alice", username);
        verify(accountsClient).transfer(req);

        ArgumentCaptor<NotificationRequest> cap = ArgumentCaptor.forClass(NotificationRequest.class);
        verify(notificationClient).notify(cap.capture());
        NotificationRequest sent = cap.getValue();
        assertThat(sent.getServiceName()).isEqualTo("transfer-service");
        assertThat(sent.getMessage()).contains("Пользователь " + username)
            .contains("выполнил перевод")
            .contains("alice")
            .contains("bob")
            .contains("50.00");
    }

    @Test
    void submit_whenNotOwner_sendsNotification_andThrowsAccessDenied() {
        TransferRequest req = new TransferRequest();
        req.setFromLogin("alice");
        req.setToLogin("bob");
        req.setAmount(new BigDecimal("50.00"));

        String username = "notOwnerUser";

        Jwt jwt = new Jwt(
            "token",
            Instant.now(),
            Instant.now().plusSeconds(3600),
            Map.of("alg", "none"),
            Map.of(
                "preferred_username", username
            )
        );
        JwtAuthenticationToken auth = new JwtAuthenticationToken(jwt);

        when(accountsClient.isOwner("alice", username)).thenReturn(false);

        AccessDeniedException ex = assertThrows(AccessDeniedException.class,
            () -> transferService.submit(req, auth));

        assertThat(ex.getMessage()).contains("не является владельцем счёта");

        verify(accountsClient).isOwner("alice", username);
        verify(accountsClient, never()).transfer(any());

        ArgumentCaptor<NotificationRequest> cap = ArgumentCaptor.forClass(NotificationRequest.class);
        verify(notificationClient).notify(cap.capture());
        NotificationRequest sent = cap.getValue();
        assertThat(sent.getServiceName()).isEqualTo("transfer-service");
        assertThat(sent.getMessage()).contains("Попытка несанкционированного перевода")
            .contains("alice")
            .contains(username);
    }
}
