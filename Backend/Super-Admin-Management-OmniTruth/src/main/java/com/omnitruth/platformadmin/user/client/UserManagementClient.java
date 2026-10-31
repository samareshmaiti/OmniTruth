package com.omnitruth.platformadmin.user.client;

import java.util.UUID;

import com.omnitruth.platformadmin.tenant.dto.CompanyAdminInvitationResponse;

public interface UserManagementClient {
    CompanyAdminInvitationResponse inviteCompanyAdmin(UUID tenantId, String email, String invitedBy);
}
