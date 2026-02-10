package yandex.workshop.notificationsservice.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import yandex.workshop.notifications_service.api.accounts.model.NotificationRequest;

@Slf4j
@Service
public class NotificationService {
    public void notify(NotificationRequest request) {
        log.info("Notification from {}: {}",request.getServiceName(), request.getMessage());
    }
}
