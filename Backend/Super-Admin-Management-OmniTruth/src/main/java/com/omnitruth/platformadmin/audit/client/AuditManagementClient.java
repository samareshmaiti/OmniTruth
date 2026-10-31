package com.omnitruth.platformadmin.audit.client;

import java.util.Map;
import java.util.UUID;

public interface AuditManagementClient {
    void record(String actorSubject, String action, UUID tenantId, Map<String, Object> details);
}
