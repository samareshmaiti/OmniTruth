package com.omnitruth.tenant.management.config;

import com.omnitruth.tenant.management.domain.CompanyAdminInvitationRequestedEvent;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.support.serializer.ErrorHandlingDeserializer;
import org.springframework.kafka.support.serializer.JacksonJsonDeserializer;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class KafkaConsumerConfig {

    @Value("${spring.kafka.bootstrap-servers:localhost:9092}")
    private String bootstrapServers;

    @Value("${spring.kafka.consumer.group-id:omnitruth-tenant-management-group}")
    private String groupId;

    @Value("${spring.kafka.consumer.auto-offset-reset:earliest}")
    private String autoOffsetReset;

    @Bean
    public ConsumerFactory<String, CompanyAdminInvitationRequestedEvent> companyAdminInvitationConsumerFactory() {

        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);

        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, autoOffsetReset);
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
        var keyDeserializer = new ErrorHandlingDeserializer<>(new StringDeserializer());

        var valueDeserializer = new ErrorHandlingDeserializer<>(new JacksonJsonDeserializer<>(
                                CompanyAdminInvitationRequestedEvent.class).ignoreTypeHeaders());

        return new DefaultKafkaConsumerFactory<>(props, keyDeserializer, valueDeserializer);
    }

    @Bean(name = "companyAdminInvitationKafkaListenerFactory")
    public ConcurrentKafkaListenerContainerFactory<String, CompanyAdminInvitationRequestedEvent> companyAdminInvitationKafkaListenerFactory() {

        var factory = new ConcurrentKafkaListenerContainerFactory<String, CompanyAdminInvitationRequestedEvent>();
        factory.setConsumerFactory(companyAdminInvitationConsumerFactory());
        return factory;
    }
}
