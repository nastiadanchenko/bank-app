package yandex.workshop.notificationsservice;

import static org.assertj.core.api.Assertions.assertThat;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import yandex.workshop.notifications_service.api.accounts.model.NotificationRequest;
import yandex.workshop.notificationsservice.service.NotificationService;

public class NotificationServiceTest {

    private NotificationService notificationService;
    private ListAppender<ILoggingEvent> listAppender;

    @BeforeEach
    void setUp() {
        notificationService = new NotificationService();

        Logger logger = (Logger) LoggerFactory.getLogger(NotificationService.class);
        listAppender = new ListAppender<>();
        listAppender.start();
        logger.addAppender(listAppender);
    }

    @Test
    void notify_logsMessageWithServiceNameAndText() {
        NotificationRequest req = new NotificationRequest()
            .serviceName("test-service")
            .message("Some important event");

        notificationService.notify(req);

        assertThat(listAppender.list)
            .isNotEmpty()
            .anySatisfy(event -> {
                String formatted = event.getFormattedMessage();
                assertThat(formatted).contains("Notification from test-service");
                assertThat(formatted).contains("Some important event");
            });
    }
}
