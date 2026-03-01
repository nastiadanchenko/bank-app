package yandex.workshop.notificationsservice;

import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.context.annotation.Import;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.shaded.org.awaitility.Awaitility;
import yandex.workshop.api.model.NotificationRequest;
import yandex.workshop.notificationsservice.service.NotificationConsumer;


@Testcontainers
@SpringBootTest
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
@DirtiesContext
public class NotificationKafkaConsumerIT {

    @Value("${topic.notification}")
    public String topicName;

    @Autowired
    private KafkaTemplate<String, NotificationRequest> kafkaTemplate;

    @SpyBean
    private NotificationConsumer notificationConsumer;

    @Test
    void shouldConsumeNotification() throws Exception {

        NotificationRequest request = new NotificationRequest();

        request.setServiceName("accounts-service");
        request.setUserId("user-1");
        request.setMessage("Account created");
        request.setTimestamp(System.currentTimeMillis());

        kafkaTemplate.send(topicName, request);

        Awaitility.await()
            .atMost(Duration.ofSeconds(10))
            .untilAsserted(() ->
                Mockito.verify(notificationConsumer)
                    .consume(Mockito.any(NotificationRequest.class))
            );

    }

}
