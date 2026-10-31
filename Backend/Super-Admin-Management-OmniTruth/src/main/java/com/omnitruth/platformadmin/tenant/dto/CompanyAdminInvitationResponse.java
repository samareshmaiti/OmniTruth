package com.omnitruth.platformadmin.tenant.dto;

import java.time.Instant;
import java.util.UUID;

public record CompanyAdminInvitationResponse(UUID invitationId, UUID tenantId, String invitedEmail,
        String status, Instant expiresAt, String message) { }
