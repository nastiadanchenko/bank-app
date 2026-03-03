package yandex.workshop.cashservice.config;

import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.KafkaTemplate;
import yandex.workshop.api.model.NotificationRequest;
import yandex.workshop.sharedkafka.NotificationProducer;

@Configuration
public class KafkaSharedConfig {
    @Bean
    public NotificationProducer notificationProducer(
        KafkaTemplate<String, NotificationRequest> kafkaTemplate,
        MeterRegistry meterRegistry
    ) {
        return new NotificationProducer(kafkaTemplate, meterRegistry);
    }
}
