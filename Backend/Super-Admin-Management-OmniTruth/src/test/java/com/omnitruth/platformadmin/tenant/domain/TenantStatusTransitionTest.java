//package com.omnitruth.platformadmin.tenant.domain;
//
//import static org.junit.jupiter.api.Assertions.assertEquals;
//import static org.junit.jupiter.api.Assertions.assertThrows;
//
//import org.junit.jupiter.api.Test;
//
//class TenantStatusTransitionTest {
//    @Test
//    void allowsExpectedOnboardingProgression() {
//        Tenant tenant = Tenant.create("acme", "Acme Ltd", "Logistics", "platform-admin-subject");
//        tenant.changeStatus(TenantStatus.SSO_CONFIGURING);
//        tenant.changeStatus(TenantStatus.PROVISIONING);
//        tenant.changeStatus(TenantStatus.ACTIVE);
//        assertEquals(TenantStatus.ACTIVE, tenant.getStatus());
//    }
//
//    @Test
//    void closedTenantCannotBeReactivated() {
//        Tenant tenant = Tenant.create("acme", "Acme Ltd", null, "platform-admin-subject");
//        tenant.changeStatus(TenantStatus.CLOSED);
//        assertThrows(IllegalArgumentException.class, () -> tenant.changeStatus(TenantStatus.ACTIVE));
//    }
//}
