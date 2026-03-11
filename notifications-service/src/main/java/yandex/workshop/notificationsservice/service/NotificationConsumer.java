package yandex.workshop.notificationsservice.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import yandex.workshop.api.model.NotificationRequest;

@Service
@Slf4j
public class NotificationConsumer {

    @KafkaListener(
        topics = "${topic.notification}",
        groupId = "${spring.kafka.consumer.group-id}"
    )
    public void consume(NotificationRequest event) {

        log.info("Notification received service={} user={} message={}",
            event.getServiceName(),
            event.getLogin(),
            event.getMessage());

    }

}

