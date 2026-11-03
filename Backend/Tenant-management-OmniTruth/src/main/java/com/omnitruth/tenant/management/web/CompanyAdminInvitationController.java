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
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/public/v1/invitations")
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
            @Valid @RequestBody CompleteOnboardingRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        return noStore(onboardingService.completeOnboarding(request.invitationToken(), jwt));
    }

    private <T> ResponseEntity<T> noStore(T body) {
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .header(HttpHeaders.PRAGMA, "no-cache")
                .body(body);
    }
}
