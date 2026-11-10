package com.omnitruth.tenant.management.config;

import com.omnitruth.tenant.management.domain.TenantClient;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.logging.Logger;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class TenantClientProducer {

    @Value("${spring.kafka.producer.group-id:omnitruth-tenant-client-management-group}")
    private String tenantClientInvitationTopic;

    private final KafkaTemplate<String, TenantClient> kafkaTemplate;
    public TenantClientProducer(KafkaTemplate<String, TenantClient> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void publish(TenantClient event) {

        try {
            kafkaTemplate.send(
                    tenantClientInvitationTopic,
                    event.getTenantId().toString(),
                    event
            ).whenComplete((result, exception) -> {

                if (exception != null) {
                    log.error(
                            "Failed to publish company admin invitation for tenant: {}",
                            event.getTenantId(),
                            exception
                    );
                } else {
                    log.info(
                            "Company admin invitation published successfully. Tenant: {}",
                            event.getTenantId()
                    );
                }
            });

        } catch (Exception exception) {
            log.error(
                    "Error while sending company admin invitation event for tenant: {}",
                    event.getTenantId(),
                    exception
            );

            throw new RuntimeException(
                    "Unable to publish company admin invitation event",
                    exception
            );
        }
    }
}
