package yandex.workshop.sharedkafka;

import java.util.concurrent.CompletableFuture;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;
import yandex.workshop.api.model.NotificationRequest;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationProducer {
    private final KafkaTemplate<String, NotificationRequest> kafkaTemplate;

    public CompletableFuture<SendResult<String, NotificationRequest>> send(NotificationRequest event, String topicName) {

        return kafkaTemplate.send(topicName, event)
            .whenComplete((result, ex) -> {
                if (ex != null) {
                    log.error("Kafka send failed", ex);
                } else {
                    log.info("Kafka send success: {}", result.getRecordMetadata());
                }
            });

    }

}
