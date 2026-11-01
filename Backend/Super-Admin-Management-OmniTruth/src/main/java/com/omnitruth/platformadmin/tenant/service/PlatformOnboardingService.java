package com.omnitruth.platformadmin.tenant.service;

import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.omnitruth.platformadmin.audit.client.AuditManagementClient;
import com.omnitruth.platformadmin.common.controller.ConflictException;
import com.omnitruth.platformadmin.tenant.domain.TenantStatus;
import com.omnitruth.platformadmin.tenant.dto.CompanyAdminInvitationResponse;
import com.omnitruth.platformadmin.tenant.repository.TenantRepository;
import com.omnitruth.platformadmin.user.client.UserManagementClient;

@Service
public class PlatformOnboardingService {
    private final TenantRepository tenantRepository;
    private final UserManagementClient userManagementClient;
    private final AuditManagementClient auditClient;

    public PlatformOnboardingService(TenantRepository tenantRepository,
            UserManagementClient userManagementClient, AuditManagementClient auditClient) {
        this.tenantRepository = tenantRepository;
        this.userManagementClient = userManagementClient;
        this.auditClient = auditClient;
    }

    @Transactional(readOnly = true)
    public CompanyAdminInvitationResponse inviteFirstCompanyAdmin(UUID tenantId, String email, String actorSubject) {
        var tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new com.omnitruth.platformadmin.common.controller.ResourceNotFoundException("Tenant not found: " + tenantId));
        if (tenant.getStatus() != TenantStatus.SSO_CONFIGURING) {
            throw new ConflictException("The first Company Admin can be invited only while tenant SSO is being configured");
        }
        CompanyAdminInvitationResponse response = userManagementClient.inviteCompanyAdmin(tenantId, email, actorSubject);
        auditClient.record(actorSubject, "COMPANY_ADMIN_INVITATION_CREATED", tenantId,
                Map.of("invitedEmail", email, "role", "COMPANY_ADMIN"));
        return response;
    }
}
