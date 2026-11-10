export interface PlatformApiTokenClaims {
    aud?: string;
    iss?: string;
    ver?: string;
    roles?: string[];
    scp?: string;
    tid?: string;
    oid?: string;
    sub?: string;
    exp?: number;
  }
  
  export interface TenantResolutionResponse {
    tenantId: string;
    tenantSlug: string;
    companyName: string;
    entraTenantId: string;
    status: string;
  }
  
  export interface TenantDashboardResponse {
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
  
  export interface LoginState {
    flow: 'auto' | 'platform' | 'customer';
    tenantSlug?: string;
    email?: string;
  }
  
  export interface LoginOptions {
    loginHint?: string;
    prompt?: 'select_account' | 'login' | 'none';
  }