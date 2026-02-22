package yandex.workshop.transferservice;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import java.time.Duration;
import java.util.Map;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.kafka.KafkaContainer;
import yandex.workshop.transfer_service.api.accounts.model.NotificationRequest;
import yandex.workshop.transferservice.service.NotificationProducer;


@Testcontainers
@SpringBootTest
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
public class NotificationKafkaProducerIT {

    @Value("${topic.notification}")
    public String testTopicName;

    @Autowired
    private NotificationProducer notificationProducer;

    private Consumer<String, NotificationRequest> consumer;

    @Autowired
    private KafkaContainer kafkaContainer;
    @BeforeEach
    void setup() {

        Map<String, Object> props =
            KafkaTestUtils.consumerProps(
                kafkaContainer.getBootstrapServers(),
                "test-group", "true"
            );
        consumer =
            new DefaultKafkaConsumerFactory<>(
                props,
                new StringDeserializer(),
                new JsonDeserializer<>(
                    NotificationRequest.class,
                    false)
            ).createConsumer();

        consumer.subscribe(
            java.util.Collections.singleton(testTopicName)
        );
    }


    @Test
    void shouldSendNotificationToKafka() {

        NotificationRequest request =
            new NotificationRequest();

        request.setServiceName("transfer-service");
        request.setUserId("user-1");
        request.setMessage("Transfer money from account A to account B");
        request.setTimestamp(System.currentTimeMillis());

        notificationProducer.send(request);

        ConsumerRecords<String, NotificationRequest> records =
            consumer.poll(Duration.ofSeconds(5));

        assertThat(records.count())
            .isGreaterThan(0);

        NotificationRequest actual =
            records.iterator().next().value();

        assertThat(actual.getUserId())
            .isEqualTo("user-1");

        assertThat(actual.getMessage())
            .isEqualTo("Transfer money from account A to account B");

        assertThat(actual.getServiceName())
            .isEqualTo("transfer-service");

    }

}
