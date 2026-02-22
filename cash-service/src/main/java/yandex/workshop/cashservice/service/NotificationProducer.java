package yandex.workshop.cashservice.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import yandex.workshop.cashservice.api.accounts.model.NotificationRequest;


@Service
@RequiredArgsConstructor
public class NotificationProducer {
    private final KafkaTemplate<String, NotificationRequest> kafkaTemplate;

    @Value("${topic.notification}")
    private String topicName;
    public void send(NotificationRequest event) {

        kafkaTemplate.send(topicName, event);

    }

}
