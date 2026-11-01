package com.omnitruth.platformadmin.user.client;

import java.util.UUID;

import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import com.omnitruth.platformadmin.tenant.dto.CompanyAdminInvitationResponse;

@Component
public class RestUserManagementClient implements UserManagementClient {
    private final WebClient webClient;

    public RestUserManagementClient(WebClient userManagementWebClient) {
        this.webClient = userManagementWebClient;
    }

    @Override
    public CompanyAdminInvitationResponse inviteCompanyAdmin(UUID tenantId, String email, String invitedBy) {
        InvitationCommand command = new InvitationCommand(tenantId, email, "COMPANY_ADMIN", invitedBy);
        return webClient.post()
                .uri("/api/v1/platform-invitations")
                .bodyValue(command)
                .retrieve()
                .bodyToMono(CompanyAdminInvitationResponse.class)
                .block();
    }

    private record InvitationCommand(UUID tenantId, String email, String requestedRole, String invitedBy) { }
}
