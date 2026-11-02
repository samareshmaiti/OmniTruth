
package com.omnitruth.platformadmin.tenant.service;

import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import com.omnitruth.platformadmin.tenant.dto.CompanyAdminInvitationRequestedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.omnitruth.platformadmin.common.controller.ConflictException;
import com.omnitruth.platformadmin.common.controller.ResourceNotFoundException;
import com.omnitruth.platformadmin.tenant.domain.TenantStatus;
import com.omnitruth.platformadmin.tenant.dto.InvitationAcceptedResponse;
import com.omnitruth.platformadmin.tenant.repository.TenantRepository;

@Service
public class PlatformOnboardingService {

    private static final Logger log =
            LoggerFactory.getLogger(PlatformOnboardingService.class);

    private final TenantRepository tenantRepository;
    private final KafkaTemplate<String, CompanyAdminInvitationRequestedEvent>
            kafkaTemplate;
    private final String invitationTopic;

    public PlatformOnboardingService(
            TenantRepository tenantRepository,
            KafkaTemplate<String, CompanyAdminInvitationRequestedEvent> kafkaTemplate,
            @Value("${omnitruth.kafka.topics.company-admin-invitations}")
            String invitationTopic) {

        this.tenantRepository = tenantRepository;
        this.kafkaTemplate = kafkaTemplate;
        this.invitationTopic = invitationTopic;
    }

    /**
     * Validates the tenant and asynchronously requests a Company Admin invitation.
     */
    @Transactional(readOnly = true)
    public InvitationAcceptedResponse inviteFirstCompanyAdmin(
            UUID tenantId,
            String email,
            String actorSubject) {

        var tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Tenant not found: " + tenantId
                        )
                );

        if (tenant.getStatus() != TenantStatus.SSO_CONFIGURING) {
            throw new ConflictException(
                    "The first Company Admin can be invited only while "
                            + "tenant SSO is being configured"
            );
        }


        UUID eventId = UUID.randomUUID();

        CompanyAdminInvitationRequestedEvent event =
                new CompanyAdminInvitationRequestedEvent(
                        eventId,
                        "company.admin.invitation.requested",
                        1,
                        tenantId,
                        tenant.getTenantCode(),
                        email,
                        "COMPANY_ADMIN",
                        actorSubject,
                        Instant.now()
                );

        CompletableFuture<?> publishResult =
                kafkaTemplate.send(
                        invitationTopic,
                        tenantId.toString(),
                        event
                );

        publishResult.whenComplete((result, exception) -> {
            if (exception != null) {
                log.error(
                        "Failed to publish Company Admin invitation event. "
                                + "Event ID: {}, Tenant ID: {}",
                        eventId,
                        tenantId,
                        exception
                );
            } else {
                log.info(
                        "Company Admin invitation event published. "
                                + "Event ID: {}, Tenant ID: {}",
                        eventId,
                        tenantId
                );
            }
        });

        return new InvitationAcceptedResponse(
                eventId,
                tenantId,
                email,
                "COMPANY_ADMIN",
                "ACCEPTED",
                "Company Admin invitation request accepted for processing",
                Instant.now()
        );
    }
}
