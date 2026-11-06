import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { MsalService } from '@azure/msal-angular';
import { AccountInfo } from '@azure/msal-browser';
import { firstValueFrom } from 'rxjs';
import { environment } from '../../../environment';

export type AccessStatus = 'ACTIVE' | 'SUSPENDED' | 'REVOKED';

export interface TenantBootstrapContext {
  // The current Tenant Management API does not return a separate user ID.
  userId?: string;
  tenantId: string;
  tenantSlug: string;
  companyName: string;
  roles: string[];
  accessStatus: AccessStatus;
  landingPath?: string;
}

interface TenantDashboardResponse {
  tenant: {
    tenantId: string;
    tenantCode: string;
    companyName: string;
    status: string;
  };
  totalClients: number;
  activeClients: number;
  totalApiEndpoints: number;
  enabledApiEndpoints: number;
  totalTools: number;
  enabledTools: number;
}

@Injectable({ providedIn: 'root' })
export class TenantContextService {
  private readonly http = inject(HttpClient);
  private readonly msal = inject(MsalService);
  private readonly baseUrl = `${environment.tenantManagement.baseUrl.replace(/\/$/, '')}/api/tenant/v1`;

  private getAccount(): AccountInfo {
    const account = this.msal.instance.getActiveAccount()
      ?? this.msal.instance.getAllAccounts()[0]
      ?? null;

    if (!account) {
      throw new Error('No Microsoft account is signed in. Please sign in again.');
    }

    return account;
  }

  /** Obtain an access token specifically for Tenant Management. */
  private async getAccessToken(): Promise<string> {
    const account = this.getAccount();
    const scope = environment.tenantManagement.scope;

    if (!scope) {
      throw new Error('Tenant Management API scope is not configured.');
    }

    const authority = account.tenantId
      ? `https://login.microsoftonline.com/${account.tenantId}`
      :  environment.azure.commonAuthority;

    const result = await firstValueFrom(
      this.msal.acquireTokenSilent({ account, authority, scopes: [scope] })
    );

    return result.accessToken;
  }

  /**
   * The backend does not expose a separate /onboarding/me/context route.
   * Its existing dashboard route is protected by COMPANY_ADMIN and resolves
   * the tenant from the validated Entra tid + oid claims.
   */
  async getMyContext(): Promise<TenantBootstrapContext> {
    const accessToken = await this.getAccessToken();

    try {
      const dashboard = await firstValueFrom(
        this.http.get<TenantDashboardResponse>(`${this.baseUrl}/dashboard`, {
          headers: { Authorization: `Bearer ${accessToken}` }
        })
      );

      const tenant = dashboard?.tenant;

      if (!tenant?.tenantId || !tenant.tenantCode || !tenant.companyName) {
        throw new Error('Tenant Management returned an incomplete dashboard response.');
      }

      if (tenant.status?.toUpperCase() !== 'ACTIVE') {
        throw new Error('Your OmniTruth organisation is not active.');
      }

      // A successful response proves the active COMPANY_ADMIN role because
      // SecurityConfig and TenantWorkspaceController enforce that role.
      return {
        tenantId: tenant.tenantId,
        tenantSlug: tenant.tenantCode,
        companyName: tenant.companyName,
        roles: ['COMPANY_ADMIN'],
        accessStatus: 'ACTIVE',
        landingPath: `/app/${tenant.tenantCode}/dashboard`
      };
    } catch (error) {
      if (error instanceof HttpErrorResponse) {
        if ([401, 403].includes(error.status)) {
          throw new Error(
            'Your Microsoft identity is not registered as an active Company Admin for an OmniTruth organisation. Complete your invitation or contact your administrator.'
          );
        }

        if (error.status === 400 || error.status === 409) {
          const detail = error.error?.detail ?? error.error?.message;
          throw new Error(
            detail || 'Your Microsoft identity is not linked to an active OmniTruth organisation.'
          );
        }
      }

      throw error;
    }
  }

  /** Check the existing Tenant Management authorization, not User Management/Kafka. */
  async waitForCompanyAdminAccess(maxAttempts = 5, intervalMs = 1000): Promise<TenantBootstrapContext> {
    for (let attempt = 0; attempt < maxAttempts; attempt++) {
      try {
        const context = await this.getMyContext();

        if (context.accessStatus === 'ACTIVE' && context.roles.includes('COMPANY_ADMIN')) {
          return context;
        }

        if (context.accessStatus === 'SUSPENDED' || context.accessStatus === 'REVOKED') {
          throw new Error('Your Company Admin access is suspended or revoked. Contact OmniTruth support.');
        }
      } catch (error) {
        if (error instanceof HttpErrorResponse && [404, 425, 503].includes(error.status)) {
          await this.delay(intervalMs);
          continue;
        }

        if (error instanceof HttpErrorResponse && error.status === 0) {
          throw new Error(
            'Cannot connect to Tenant Management. Check that the service is running and CORS is configured for localhost:4200.'
          );
        }

        throw error;
      }

      await this.delay(intervalMs);
    }

    throw new Error('Your Company Admin access is not yet active. Please try checking again.');
  }

  private delay(milliseconds: number): Promise<void> {
    return new Promise(resolve => setTimeout(resolve, milliseconds));
  }
}
