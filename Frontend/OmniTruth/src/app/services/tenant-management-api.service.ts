import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { MsalService } from '@azure/msal-angular';
import { AccountInfo } from '@azure/msal-browser';
import { Observable, from, throwError } from 'rxjs';
import { catchError, map, switchMap } from 'rxjs/operators';
import { environment } from '../../../environment';

export interface TenantProfile {
  tenantId: string;
  tenantCode: string;
  companyName: string;
  industry: string | null;
  status: string;
  createdAt: string;
  updatedAt: string;
}

export interface TenantProfileRequest {
  companyName: string;
  industry: string | null;
}

export interface TenantDashboard {
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

export interface TenantClient {
  tenantClientType: TenantClientType;
  userType: ClientUserRole;
  clientId: string;
  clientName: string;
  contactEmail: string | null;
  externalReference: string | null;
  notes: string | null;
  status: 'ACTIVE' | 'INACTIVE';
  createdAt: string;
  updatedAt: string;
}

export type TenantClientType =
| 'INTERNAL_EMPLOYEE'
| 'EXTERNAL_COMPANY'
| 'INDIVIDUAL_CLIENT'
| 'BUSINESS_PARTNER'

export type ClientUserRole =
| 'CLIENT_ADMIN'
| 'CLIENT_USER'
| 'CLIENT_VIEWER'
| 'CLIENT_OPERATOR'

export interface TenantClientRequest {
  clientName: string;
  contactEmail?: string;
  externalReference?: string;
  notes?: string;
  status?: string;
  tenantClientType: TenantClientType;
  appRole: ClientUserRole;
}

export interface TenantApiEndpoint {
  endpointId: string;
  endpointName: string;
  baseUrl: string;
  httpMethod: string;
  authType: string;
  secretReference: string | null;
  description: string | null;
  enabled: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface TenantApiEndpointRequest {
  endpointName: string;
  baseUrl: string;
  httpMethod: string;
  authType: string;
  secretReference?: string;
  description?: string;
  enabled: boolean;
}

export interface TenantTool {
  toolId: string;
  endpointId: string;
  toolName: string;
  description: string;
  inputSchema: string;
  enabled: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface TenantToolRequest {
  toolName: string;
  description: string;
  endpointId: string;
  inputSchema: string;
  enabled: boolean;
}

@Injectable({ providedIn: 'root' })
export class TenantManagementApiService {
  private readonly http = inject(HttpClient);
  private readonly msal = inject(MsalService);
  private readonly baseUrl =
    `${environment.tenantManagement.baseUrl.replace(/\/$/, '')}/api/tenant/v1`;

  private accessToken(): Observable<string> {
    const account: AccountInfo | null =
      this.msal.instance.getActiveAccount() ??
      this.msal.instance.getAllAccounts()[0] ??
      null;

    if (!account) {
      return throwError(() => new Error('Your Microsoft session is missing. Please sign in again.'));
    }

    return from(this.msal.instance.acquireTokenSilent({
      account,
      scopes: [environment.platformApi.scope]
    })).pipe(
      map(result => result.accessToken),
      catchError(() => throwError(() =>
        new Error('Unable to acquire an access token for Tenant Management. Please sign in again.')
      ))
    );
  }

  private request<T>(call: (headers: HttpHeaders) => Observable<T>): Observable<T> {
    return this.accessToken().pipe(
      switchMap(token => call(new HttpHeaders({ Authorization: `Bearer ${token}` })))
    );
  }

  profile(): Observable<TenantProfile> {
    return this.request(headers => this.http.get<TenantProfile>(`${this.baseUrl}/profile`, { headers }));
  }

  updateProfile(body: TenantProfileRequest): Observable<TenantProfile> {
    return this.request(headers => this.http.put<TenantProfile>(`${this.baseUrl}/profile`, body, { headers }));
  }

  dashboard(): Observable<TenantDashboard> {
    return this.request(headers => this.http.get<TenantDashboard>(`${this.baseUrl}/dashboard`, { headers }));
  }

  clients(): Observable<TenantClient[]> {
    return this.request(headers => this.http.get<TenantClient[]>(`${this.baseUrl}/clients`, { headers }));
  }

  createClient(body: TenantClientRequest): Observable<TenantClient> {
    return this.request(headers => this.http.post<TenantClient>(`${this.baseUrl}/clients`, body, { headers }));
  }

  updateClient(id: string, body: TenantClientRequest): Observable<TenantClient> {
    return this.request(headers => this.http.put<TenantClient>(`${this.baseUrl}/clients/${encodeURIComponent(id)}`, body, { headers }));
  }

  endpoints(): Observable<TenantApiEndpoint[]> {
    return this.request(headers => this.http.get<TenantApiEndpoint[]>(`${this.baseUrl}/api-endpoints`, { headers }));
  }

  createEndpoint(body: TenantApiEndpointRequest): Observable<TenantApiEndpoint> {
    return this.request(headers => this.http.post<TenantApiEndpoint>(`${this.baseUrl}/api-endpoints`, body, { headers }));
  }

  updateEndpoint(id: string, body: TenantApiEndpointRequest): Observable<TenantApiEndpoint> {
    return this.request(headers => this.http.put<TenantApiEndpoint>(`${this.baseUrl}/api-endpoints/${encodeURIComponent(id)}`, body, { headers }));
  }

  tools(): Observable<TenantTool[]> {
    return this.request(headers => this.http.get<TenantTool[]>(`${this.baseUrl}/tools`, { headers }));
  }

  createTool(body: TenantToolRequest): Observable<TenantTool> {
    return this.request(headers => this.http.post<TenantTool>(`${this.baseUrl}/tools`, body, { headers }));
  }

  updateTool(id: string, body: TenantToolRequest): Observable<TenantTool> {
    return this.request(headers => this.http.put<TenantTool>(`${this.baseUrl}/tools/${encodeURIComponent(id)}`, body, { headers }));
  }
}
