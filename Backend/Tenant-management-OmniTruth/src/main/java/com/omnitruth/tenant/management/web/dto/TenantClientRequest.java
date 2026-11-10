package com.omnitruth.tenant.management.web.dto;
import com.omnitruth.tenant.management.utility.ClientUserRole;
import com.omnitruth.tenant.management.utility.TenantClientType;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@ToString
@NoArgsConstructor
public class TenantClientRequest {
    @NotBlank @Size(max=180)
    private String clientName;
    @Email @Size(max=254)
    private String contactEmail;
    @Size(max=120)
    private String externalReference;
    @Size(max=2000)
    private String notes;
    @NotBlank
    private String status;
    @NotNull TenantClientType tenantClientType;
    @NotNull ClientUserRole appRole;
}
