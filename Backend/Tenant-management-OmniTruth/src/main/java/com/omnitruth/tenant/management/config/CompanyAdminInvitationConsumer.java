package com.omnitruth.tenant.management.config;

import com.omnitruth.tenant.management.domain.CompanyAdminInvitationRequestedEvent;
import com.omnitruth.tenant.management.service.CompanyAdminInvitationService;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class CompanyAdminInvitationConsumer {

    private final CompanyAdminInvitationService invitationService;

    public CompanyAdminInvitationConsumer(
            CompanyAdminInvitationService invitationService) {
        this.invitationService = invitationService;
    }

    @KafkaListener(
            topics = "${omnitruth.kafka.topics.company-admin-invitations}",
            groupId = "${spring.kafka.consumer.group-id}",
            containerFactory = "companyAdminInvitationKafkaListenerFactory"
    )
    public void consume(CompanyAdminInvitationRequestedEvent event) {
        invitationService.processInvitation(event);
    }
}
