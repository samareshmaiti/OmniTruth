import { Injectable, inject } from '@angular/core';
import {
  HttpClient,
  HttpHeaders,
  HttpParams
} from '@angular/common/http';
import { MsalService } from '@azure/msal-angular';
import { AccountInfo } from '@azure/msal-browser';
import { Observable,forkJoin,from,throwError } from 'rxjs';
import { catchError,map,switchMap } from 'rxjs/operators';
import { environment } from './environment';

export type TenantStatus =
  | 'PENDING'
  | 'SSO_CONFIGURING'
  | 'PROVISIONING'
  | 'ACTIVE'
  | 'SUSPENDED'
  | 'CLOSED';

export interface Tenant {
  tenantId: string;
  tenantCode: string;
  companyName: string;
  industry: string | null;
  status: TenantStatus;
  createdAt: string;
  updatedAt: string;
}

/** Spring Data Page<T> response returned by the current backend. */
export interface PageResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
  first?: boolean;
  last?: boolean;
  numberOfElements?: number;
  empty?: boolean;
}

export interface CreateTenantRequest {
  tenantCode: string;
  companyName: string;
  industry?: string;
}

export interface UpdateTenantStatusRequest {
  status: TenantStatus;
}

export interface CompanyAdminInvitationResponse {
  invitationId: string;
  tenantId: string;
  invitedEmail: string;
  status: string;
  expiresAt: string;
  message: string;
}

export interface DashboardSummary {
  totalTenants: number;
  pending: number;
  ssoConfiguring: number;
  provisioning: number;
  active: number;
  suspended: number;
  closed: number;
  onboarding: number;
}

export interface ActuatorHealthResponse {
  status: string;
  components?: Record<string, unknown>;
}

@Injectable({ providedIn: 'root' })
export class ApiServices {
  private readonly http = inject(HttpClient);
  private readonly msal = inject(MsalService);

  private readonly baseUrl = environment.platformApiBaseUrl.replace(/\/$/, '');
  private readonly apiScope = environment.platformApiScope;
  private readonly tenantsUrl = `${this.baseUrl}/api/platform/v1/tenants`;

  /**
   * Gets a Platform API access token from Microsoft Entra through MSAL.
   * The token is not stored in this service or in a custom localStorage key.
   */
  private getAccessToken(): Observable<string> {
    const account: AccountInfo | null =
      this.msal.instance.getActiveAccount() ??
      this.msal.instance.getAllAccounts()[0] ??
      null;

    if (!account) {
      return throwError(() => new Error(
        'No Microsoft account is active. Sign in before calling the Platform API.'
      ));
    }

    return from(this.msal.instance.acquireTokenSilent({
      account,
      scopes: [this.apiScope]
    })).pipe(
      map(result => result.accessToken),
      catchError(error => {
        const detail = error instanceof Error ? error.message : 'Interactive authentication may be required.';
        return throwError(() => new Error(
          `Unable to acquire a Platform API access token. ${detail}`
        ));
      })
    );
  }

  /** Adds the Microsoft Bearer token to each protected API request. */
  private withAuthorization<T>(
    request: (headers: HttpHeaders) => Observable<T>
  ): Observable<T> {
    return this.getAccessToken().pipe(
      switchMap(token => request(new HttpHeaders({
        Authorization: `Bearer ${token}`
      })))
    );
  }

  /**
   * GET /api/platform/v1/tenants?page=0&size=10
   * Optional status filter is supported by the current backend.
   */
  getTenants(
    status?: TenantStatus | '',
    page = 0,
    size = 10
  ): Observable<PageResponse<Tenant>> {
    let params = new HttpParams()
      .set('page', page)
      .set('size', size)
      .set('sort', 'createdAt,desc');

    if (status) {
      params = params.set('status', status);
    }

    return this.withAuthorization(headers =>
      this.http.get<PageResponse<Tenant>>(this.tenantsUrl, { headers, params })
    );
  }

  /** GET /api/platform/v1/tenants/{tenantId} */
  getTenant(tenantId: string): Observable<Tenant> {
    return this.withAuthorization(headers =>
      this.http.get<Tenant>(`${this.tenantsUrl}/${encodeURIComponent(tenantId)}`, { headers })
    );
  }

  /** POST /api/platform/v1/tenants */
  createTenant(request: CreateTenantRequest): Observable<Tenant> {
    return this.withAuthorization(headers =>
      this.http.post<Tenant>(this.tenantsUrl, request, { headers })
    );
  }

  /** PATCH /api/platform/v1/tenants/{tenantId}/status */
  updateTenantStatus(
    tenantId: string,
    status: TenantStatus
  ): Observable<Tenant> {
    const request: UpdateTenantStatusRequest = { status };

    return this.withAuthorization(headers =>
      this.http.patch<Tenant>(
        `${this.tenantsUrl}/${encodeURIComponent(tenantId)}/status`,
        request,
        { headers }
      )
    );
  }

  /** POST /api/platform/v1/tenants/{tenantId}/company-admin-invitations */
  inviteCompanyAdmin(
    tenantId: string,
    email: string
  ): Observable<CompanyAdminInvitationResponse> {
    return this.withAuthorization(headers =>
      this.http.post<CompanyAdminInvitationResponse>(
        `${this.tenantsUrl}/${encodeURIComponent(tenantId)}/company-admin-invitations`,
        { email },
        { headers }
      )
    );
  }

  /**
   * The current backend has no aggregate dashboard endpoint.
   * Read totalElements from the server-side filtered pages to build the cards.
   */
  getDashboardSummary(): Observable<DashboardSummary> {
    return forkJoin({
      all: this.getTenants('', 0, 1),
      pending: this.getTenants('PENDING', 0, 1),
      ssoConfiguring: this.getTenants('SSO_CONFIGURING', 0, 1),
      provisioning: this.getTenants('PROVISIONING', 0, 1),
      active: this.getTenants('ACTIVE', 0, 1),
      suspended: this.getTenants('SUSPENDED', 0, 1),
      closed: this.getTenants('CLOSED', 0, 1)
    }).pipe(
      map(result => {
        const pending = result.pending.totalElements;
        const ssoConfiguring = result.ssoConfiguring.totalElements;
        const provisioning = result.provisioning.totalElements;
        const suspended = result.suspended.totalElements;
        const closed = result.closed.totalElements;

        return {
          totalTenants: result.all.totalElements,
          pending,
          ssoConfiguring,
          provisioning,
          active: result.active.totalElements,
          suspended,
          closed,
          onboarding: pending + ssoConfiguring + provisioning
        };
      })
    );
  }

  /** Public Spring Boot Actuator health endpoint (does not require a JWT in the starter). */
  getBackendHealth(): Observable<ActuatorHealthResponse> {
    return this.http.get<ActuatorHealthResponse>(`${this.baseUrl}/actuator/health`);
  }
}
