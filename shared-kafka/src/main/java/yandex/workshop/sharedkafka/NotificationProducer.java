package yandex.workshop.sharedkafka;

import io.micrometer.core.instrument.MeterRegistry;
import java.util.concurrent.CompletableFuture;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;
import yandex.workshop.api.model.NotificationRequest;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationProducer {
    private final KafkaTemplate<String, NotificationRequest> kafkaTemplate;

    private final MeterRegistry meterRegistry;

    public CompletableFuture<SendResult<String, NotificationRequest>> send(NotificationRequest event,
                                                                           String topicName) {

        log.debug("Preparing notification event service={} user={} topic={} message={}",
            event.getServiceName(),
            event.getLogin(),
            topicName,
            event.getMessage());

        log.info("Sending notification to Kafka topic={} user={}",
            topicName,
            event.getLogin());

        return kafkaTemplate.send(topicName, event)
            .whenComplete((result, ex) -> {
                if (ex != null) {
                    log.error("Kafka send failed topic={} user={} error={}",
                        topicName,
                        event.getLogin(),
                        ex.getMessage(),
                        ex);
                    meterRegistry.counter(
                        "business.notification.failed",
                        "login", event.getLogin()
                    ).increment();

                } else {
                    RecordMetadata metadata = result.getRecordMetadata();
                    log.info("Kafka send success topic={} partition={} offset={} user={}",
                        metadata.topic(),
                        metadata.partition(),
                        metadata.offset(),
                        event.getLogin());
                }
            });

    }

}
