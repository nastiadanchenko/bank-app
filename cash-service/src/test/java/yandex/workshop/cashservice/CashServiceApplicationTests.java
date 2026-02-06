package yandex.workshop.cashservice;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(properties = "spring.cloud.config.enabled=false")
@ActiveProfiles("test")
class CashServiceApplicationTests {

    @Test
    void contextLoads() {
    }

}
