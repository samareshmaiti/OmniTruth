
import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { MsalService } from '@azure/msal-angular';
import { AccountInfo } from '@azure/msal-browser';
import { firstValueFrom } from 'rxjs';
import { environment } from '../../../environment';

export type AccessStatus =
  | 'PENDING'
  | 'ACTIVE'
  | 'SUSPENDED'
  | 'REVOKED';

export interface TenantBootstrapContext {
  // The User Management user ID may not exist yet.
  userId?: string;

  tenantId: string;
  tenantSlug: string;
  companyName: string;

  roles: string[];

  // Verified by Tenant Management, not by Angular.
  accessStatus: AccessStatus;

  onboardingStatus: string;

  landingPath?: string;
}

@Injectable({ providedIn: 'root' })
export class TenantContextService {

  private readonly http = inject(HttpClient);
  private readonly msal = inject(MsalService);

  // Tenant Management is responsible for initial Company Admin access.
  private readonly baseUrl =
    `${environment.tenantManagement.baseUrl.replace(/\/$/, '')}/api/tenant/v1`;

  private getAccount(): AccountInfo {

    const account =
      this.msal.instance.getActiveAccount() ??
      this.msal.instance.getAllAccounts()[0] ??
      null;

    if (!account) {
      throw new Error(
        'No Microsoft account is signed in. Please sign in again.'
      );
    }

    return account;
  }

  /**
   * Obtain an ACCESS TOKEN specifically for Tenant Management.
   * Do not send an ID token to the backend.
   */
  private async getAccessToken(): Promise<string> {

    const account = this.getAccount();

    const scope = environment.tenantManagement.scope;

    if (!scope) {
      throw new Error(
        'Tenant Management API scope is not configured.'
      );
    }

    const authority = account.tenantId
      ? `https://login.microsoftonline.com/${account.tenantId}`
      : environment.azure.organizationsAuthority;

    const result = await firstValueFrom(
      this.msal.acquireTokenSilent({
        account,
        authority,
        scopes: [scope]
      })
    );

    return result.accessToken;
  }

  /**
   * Get initial Company Admin access from Tenant Management.
   *
   * Expected endpoint:
   * GET /api/tenant/v1/onboarding/me/context
   *
   * This endpoint must verify the authenticated tid + oid,
   * accepted invitation and tenant mapping on the backend.
   */
  async getMyContext(): Promise<TenantBootstrapContext> {

    const accessToken = await this.getAccessToken();

    return firstValueFrom(
      this.http.get<TenantBootstrapContext>(
        `${this.baseUrl}/onboarding/me/context`,
        {
          headers: {
            Authorization: `Bearer ${accessToken}`
          }
        }
      )
    );
  }

  /**
   * Wait for Tenant Management to confirm initial
   * Company Admin authorization.
   *
   * This does NOT wait for User Management or Kafka consumption.
   */
  async waitForCompanyAdminAccess(
    maxAttempts = 15,
    intervalMs = 2000
  ): Promise<TenantBootstrapContext> {

    for (let attempt = 0; attempt < maxAttempts; attempt++) {

      try {

        const context = await this.getMyContext();

        if (
          context.accessStatus === 'ACTIVE' &&
          context.roles.includes('COMPANY_ADMIN')
        ) {
          return context;
        }

        if (
          context.accessStatus === 'SUSPENDED' ||
          context.accessStatus === 'REVOKED'
        ) {
          throw new Error(
            'Your Company Admin access is suspended or revoked. Contact OmniTruth support.'
          );
        }

      } catch (error) {

        if (error instanceof HttpErrorResponse) {

          // These can represent a context that has not yet
          // been committed by the onboarding operation.
          if ([404, 425, 503].includes(error.status)) {
            await this.delay(intervalMs);
            continue;
          }

          if (error.status === 0) {
            throw new Error(
              'Cannot connect to Tenant Management. Check that the service is running and CORS is configured for localhost:4200.'
            );
          }
        }

        throw error;
      }

      await this.delay(intervalMs);
    }

    throw new Error(
      'Your Company Admin access is not yet active. Please try checking again.'
    );
  }

  private delay(milliseconds: number): Promise<void> {
    return new Promise(resolve => setTimeout(resolve, milliseconds));
  }
}
