package com.omnitruth.tenant.management.web.dto.onboarding;

import java.time.Instant;
import java.util.UUID;

public record InvitationDetailsResponse(
        UUID invitationId,
        String companyName,
        String tenantCode,
        String invitedEmail,
        String requestedRole,
        String status,
        Instant expiresAt
) { }
