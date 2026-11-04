package com.omnitruth.tenant.management.web;

import com.omnitruth.tenant.management.service.CompanyAdminOnboardingService;
import com.omnitruth.tenant.management.web.dto.onboarding.AcceptInvitationRequest;
import com.omnitruth.tenant.management.web.dto.onboarding.CompleteOnboardingRequest;
import com.omnitruth.tenant.management.web.dto.onboarding.InvitationDetailsResponse;
import com.omnitruth.tenant.management.web.dto.onboarding.OnboardingCompleteResponse;
import jakarta.validation.Valid;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/public/v1/invitations")
@CrossOrigin("http://localhost:4200/")
public class CompanyAdminInvitationController {

    private final CompanyAdminOnboardingService onboardingService;

    public CompanyAdminInvitationController(CompanyAdminOnboardingService onboardingService) {
        this.onboardingService = onboardingService;
    }

    @GetMapping("/validate")
    public ResponseEntity<InvitationDetailsResponse> validate(
            @RequestParam("token") String token) {
        return noStore(onboardingService.validateInvitation(token));
    }

    @PostMapping("/accept")
    public ResponseEntity<InvitationDetailsResponse> accept(
            @Valid @RequestBody AcceptInvitationRequest request) {
        return noStore(onboardingService.acceptInvitation(request.token()));
    }

    // This endpoint requires a verified Microsoft Entra access token.
    @PostMapping("/complete")
    public ResponseEntity<OnboardingCompleteResponse> complete(
            @RequestBody @Valid CompleteOnboardingRequest request,
            @AuthenticationPrincipal Jwt jwt) {

        System.out.println("1. Controller entered");

        try {
            System.out.println("2. JWT subject: " + jwt.getSubject());
            System.out.println("3. Calling onboarding service");

            OnboardingCompleteResponse response =
                    onboardingService.completeOnboarding(
                            request.invitationToken(),
                            jwt
                    );

            System.out.println("4. Onboarding service completed");

            return ResponseEntity.ok(response);

        } catch (Exception ex) {
            System.err.println("5. Onboarding failed: "
                    + ex.getClass().getName());
            System.err.println("Error: " + ex.getMessage());

            ex.printStackTrace();
            throw ex;
        }
    }

    private <T> ResponseEntity<T> noStore(T body) {
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .header(HttpHeaders.PRAGMA, "no-cache")
                .body(body);
    }
}
