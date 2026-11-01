package com.omnitruth.platformadmin.tenant.domain;

public enum TenantStatus {
    PENDING,
    SSO_CONFIGURING,
    PROVISIONING,
    ACTIVE,
    SUSPENDED,
    CLOSED
}
