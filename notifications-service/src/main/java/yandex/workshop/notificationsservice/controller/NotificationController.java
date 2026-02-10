package yandex.workshop.notificationsservice.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import yandex.workshop.notifications_service.api.accounts.model.NotificationRequest;
import yandex.workshop.notificationsservice.service.NotificationService;

@Slf4j
@RestController
@RequestMapping("/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @PostMapping
    @PreAuthorize("hasAuthority('notifications')")
    public void notify(@RequestBody NotificationRequest request) {

        notificationService.notify(request);
    }
}
