import { Injectable, PLATFORM_ID, inject } from '@angular/core';
import { isPlatformBrowser } from '@angular/common';
import { HttpClient, HttpHeaders, HttpParams } from '@angular/common/http';
import { MsalService } from '@azure/msal-angular';
import { AccountInfo } from '@azure/msal-browser';
import { Observable, firstValueFrom } from 'rxjs';
import { environment } from '../../../environment';

export interface InvitationDetails {
  invitationId: string;
  companyName: string;
  tenantCode: string;
  invitedEmail: string;
  requestedRole: string;
  status: 'PENDING' | 'ACCEPTED_PENDING_SSO' | 'ACCEPTED' | 'EXPIRED' | 'REVOKED' | string;
  expiresAt: string;
}

export interface AcceptInvitationRequest {
  token: string;
}

export interface CompleteOnboardingResponse {
  tenantId: string;
  tenantCode: string;
  companyName: string;
  status: 'PROVISIONING' | 'COMPLETED' | string;
  message: string;
  correlationId?: string;
}

@Injectable({ providedIn: 'root' })
export class TenantOnboardingService {
  private readonly http = inject(HttpClient);
  private readonly msal = inject(MsalService);
  private readonly platformId = inject(PLATFORM_ID);

  private readonly invitationsUrl =
    `${environment.tenantManagement.baseUrl.replace(/\/$/, '')}/api/public/v1/invitations`;

  private readonly pendingTokenKey = 'omnitruth.pendingCompanyAdminInvitation';

  validateInvitation(token: string): Observable<InvitationDetails> {
    const params = new HttpParams().set('token', token);
    return this.http.get<InvitationDetails>(`${this.invitationsUrl}/validate`, { params });
  }

  acceptInvitation(token: string): Observable<InvitationDetails> {
    const body: AcceptInvitationRequest = { token };
    return this.http.post<InvitationDetails>(`${this.invitationsUrl}/accept`, body);
  }

  /**
   * Completes the Tenant Management part of onboarding after Microsoft SSO.
   * The API validates the bearer token and obtains tid/oid from its claims.
   * Angular never sends a trusted Entra tenant ID or object ID in the body.
   */
  async completeInvitation(
    invitationToken: string,
    account: AccountInfo
  ): Promise<CompleteOnboardingResponse> {
    const customerAuthority =
  `https://login.microsoftonline.com/${account.tenantId}`;
     // : environment.azure.organizationsAuthority;

     const result = await firstValueFrom(
      this.msal.acquireTokenSilent({
        account,
        authority: customerAuthority,
        scopes: [environment.tenantManagement.scope]
      })
    );

    const headers = new HttpHeaders({
      Authorization: `Bearer ${result.accessToken}`
    });

    return firstValueFrom(this.http.post<CompleteOnboardingResponse>(
      `${this.invitationsUrl}/complete`,
      { invitationToken },
      { headers }
    ));
  }

  /** Store the one-time invitation token only for this browser tab/session. */
  storePendingInvitationToken(token: string): void {
    if (isPlatformBrowser(this.platformId)) {
      sessionStorage.setItem(this.pendingTokenKey, token);
    }
  }

  getPendingInvitationToken(): string | null {
    if (!isPlatformBrowser(this.platformId)) return null;
    return sessionStorage.getItem(this.pendingTokenKey);
  }

  clearPendingInvitationToken(): void {
    if (isPlatformBrowser(this.platformId)) {
      sessionStorage.removeItem(this.pendingTokenKey);
    }
  }
}
