package yandex.workshop.notificationsservice;

import java.util.Map;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.support.serializer.JsonSerializer;
import org.testcontainers.kafka.KafkaContainer;
import org.testcontainers.utility.DockerImageName;
import yandex.workshop.api.model.NotificationRequest;

@TestConfiguration(proxyBeanMethods = false)
class TestcontainersConfiguration {

    @Bean
    @ServiceConnection
    KafkaContainer kafkaContainer() {
        return new KafkaContainer(DockerImageName.parse("apache/kafka-native:latest"));
    }

    @Bean
    public ProducerFactory<String, NotificationRequest> producerFactory(
        KafkaProperties properties, KafkaContainer kafkaContainer
    ) {

        Map<String, Object> config = properties.buildProducerProperties();

        config.put(
            ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG,
            JsonSerializer.class
        );

        config.put(
            ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG,
            StringSerializer.class
        );
        config.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG,
            kafkaContainer.getBootstrapServers());

        return new DefaultKafkaProducerFactory<>(config);
    }


    @Bean
    public KafkaTemplate<String, NotificationRequest> kafkaTemplate(
        ProducerFactory<String, NotificationRequest> producerFactory
    ) {

        return new KafkaTemplate<>(producerFactory);

    }

}
