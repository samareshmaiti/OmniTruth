package com.omnitruth.platformadmin.audit.client;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

@Component
public class RestAuditManagementClient implements AuditManagementClient {
    private final WebClient webClient;

    public RestAuditManagementClient(WebClient auditManagementWebClient) {
        this.webClient = auditManagementWebClient;
    }

    @Override
    public void record(String actorSubject, String action, UUID tenantId, Map<String, Object> details) {
        AuditEvent event = new AuditEvent(UUID.randomUUID(), actorSubject, action, tenantId, Instant.now(), details);
        webClient.post()
                .uri("/api/v1/audit-events")
                .bodyValue(event)
                .retrieve()
                .toBodilessEntity()
                .block();
    }

    private record AuditEvent(UUID eventId, String actorSubject, String action,
            UUID tenantId, Instant occurredAt, Map<String, Object> details) { }
}
