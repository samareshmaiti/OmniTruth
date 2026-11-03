package com.omnitruth.tenant.management.domain;

import java.time.Instant;
import java.util.UUID;

public record CompanyAdminInvitationRequestedEvent(
        UUID eventId,
        String eventType,
        int eventVersion,
        UUID tenantId,
        String tenantCode,
        String email,
        String requestedRole,
        String actorSubject,
        Instant occurredAt
) {
}
