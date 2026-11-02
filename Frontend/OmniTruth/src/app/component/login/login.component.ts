
import { CommonModule } from '@angular/common';
import { Component, OnInit } from '@angular/core';
import { FormsModule, NgForm } from '@angular/forms';
import {
  ActivatedRoute,
  Router,
  RouterLink
} from '@angular/router';

import { HttpClient, HttpHeaders } from '@angular/common/http';

import { MsalService } from '@azure/msal-angular';

import {
  AccountInfo,
  AuthenticationResult,
  RedirectRequest
} from '@azure/msal-browser';

import { firstValueFrom } from 'rxjs';

import { environment } from '../../../../environment';

interface PlatformApiTokenClaims {
  aud?: string;
  iss?: string;
  ver?: string;
  roles?: string[];
  scp?: string;
  tid?: string;
  oid?: string;
  exp?: number;
}

interface TenantResolutionResponse {
  tenantId: string;
  tenantSlug: string;
  companyName: string;
  entraTenantId: string;
  status: string;
}

interface UserContextResponse {
  tenantId: string;
  tenantSlug: string;
  companyName: string;
  roles: string[];
  landingPath?: string;
}

interface LoginState {
  flow: 'platform' | 'customer';
  tenantSlug?: string;
}

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './login.component.html',
  styleUrls: ['./login.component.css']
})
export class LoginComponent implements OnInit {

  email = '';
  statusMessage = '';
  errorMessage = '';
  isLoading = false;

  readonly currentYear = new Date().getFullYear();

  constructor(
    private router: Router,
    private route: ActivatedRoute,
    private http: HttpClient,
    private msalService: MsalService
  ) {}

  ngOnInit(): void {
    this.handleMicrosoftRedirect();
  }

  private handleMicrosoftRedirect(): void {

    this.isLoading = true;

    this.msalService.handleRedirectObservable().subscribe({

      next: async (result: AuthenticationResult | null) => {

        const account: AccountInfo | null =
          result?.account ??
          this.msalService.instance.getActiveAccount() ??
          this.msalService.instance.getAllAccounts()[0] ??
          null;

        if (!account) {
          this.isLoading = false;
          return;
        }

        this.msalService.instance.setActiveAccount(account);

        const loginState = this.getLoginState(result);

        try {
          if (loginState.flow === 'platform') {
            await this.handlePlatformLogin(account);
          } else {
            await this.handleCustomerLogin(
              account,
              loginState.tenantSlug!
            );
          }
        } catch (error) {
          console.error('Login processing failed:', error);
          this.isLoading = false;
          this.errorMessage =
            'Unable to complete login. Please contact your administrator.';
        }
      },

      error: (error) => {
        console.error('Microsoft redirect error:', error);
        this.isLoading = false;
        this.errorMessage =
          'Microsoft sign-in could not be completed.';
      }
    });
  }

  private getLoginState(
    result: AuthenticationResult | null
  ): LoginState {

    // First, recover the flow from the OAuth redirect state.
    if (result?.state) {
      try {
        const state = JSON.parse(result.state) as LoginState;

        if (
          state.flow === 'platform' ||
          (state.flow === 'customer' && state.tenantSlug)
        ) {
          return state;
        }
      } catch {
        console.warn('Could not parse application login state.');
      }
    }

    // Fallback for a direct customer login route.
    const tenantSlug =
      this.route.snapshot.paramMap.get('tenantSlug');

    if (tenantSlug) {
      return {
        flow: 'customer',
        tenantSlug
      };
    }

    return { flow: 'platform' };
  }

  // --------------------------------------------------
  // PLATFORM SUPER ADMIN LOGIN
  // --------------------------------------------------

  onContinue(loginForm: NgForm): void {

    if (loginForm.invalid) {
      loginForm.control.markAllAsTouched();
      return;
    }

    this.startLogin({
      flow: 'platform'
    }, environment.azure.authority);
  }

  private async handlePlatformLogin(
    account: AccountInfo
  ): Promise<void> {

    if (account.tenantId !== environment.azure.tenantId) {
      throw new Error('Invalid OmniTruth administrator tenant.');
    }

    const claims = account.idTokenClaims as {
      roles?: string[];
    } | undefined;

    const roles = claims?.roles ?? [];

    console.log('Platform user roles:', roles);

    if (!roles.includes('PLATFORM_SUPER_ADMIN')) {
      throw new Error('Platform Super Admin role not assigned.');
    }

    const apiToken = await this.acquireApiToken(
      account,
      environment.azure.authority
    );

    const apiClaims =
      this.decodeAccessTokenClaims(apiToken);

    this.validatePlatformApiClaims(
      apiClaims,
      environment.azure.tenantId
    );

    this.isLoading = false;
    this.statusMessage = 'Welcome to OmniTruth Super Admin Portal';

    await this.router.navigate(['/superAdmin']);
  }

  // --------------------------------------------------
  // CUSTOMER LOGIN
  // --------------------------------------------------

  onCustomerContinue(
    tenantSlug: string,
    loginForm?: NgForm
  ): void {

    if (loginForm && loginForm.invalid) {
      loginForm.control.markAllAsTouched();
      return;
    }

    const normalizedSlug = tenantSlug.trim().toLowerCase();

    if (!normalizedSlug) {
      this.errorMessage = 'Customer organisation is required.';
      return;
    }

    this.resolveTenantAndStartLogin(normalizedSlug);
  }

  private async resolveTenantAndStartLogin(
    tenantSlug: string
  ): Promise<void> {

    this.isLoading = true;
    this.errorMessage = '';
    this.statusMessage = 'Checking your organisation...';

    try {

      const tenant = await firstValueFrom(
        this.http.get<TenantResolutionResponse>(
          `${environment.tenantManagement.baseUrl}` +
          `/api/public/v1/tenants/resolve/${encodeURIComponent(tenantSlug)}`
        )
      );

      if (tenant.status !== 'ACTIVE') {
        throw new Error('This organisation is not active.');
      }

      if (!tenant.entraTenantId) {
        throw new Error('SSO is not configured for this organisation.');
      }

      this.startLogin(
        {
          flow: 'customer',
          tenantSlug: tenant.tenantSlug
        },
        `https://login.microsoftonline.com/${tenant.entraTenantId}`
      );

    } catch (error) {

      console.error('Tenant resolution failed:', error);

      this.isLoading = false;
      this.statusMessage = '';
      this.errorMessage =
        'Unable to find an active organisation with configured SSO.';
    }
  }

  private async handleCustomerLogin(
    account: AccountInfo,
    tenantSlug: string
  ): Promise<void> {

    this.statusMessage = 'Verifying your organisation...';

    // Resolve the trusted tenant configuration again.
    const tenant = await firstValueFrom(
      this.http.get<TenantResolutionResponse>(
        `${environment.tenantManagement.baseUrl}` +
        `/api/public/v1/tenants/resolve/${encodeURIComponent(tenantSlug)}`
      )
    );

    if (tenant.status !== 'ACTIVE') {
      throw new Error('Customer organisation is not active.');
    }

    // Never accept a different Entra tenant after sign-in.
    if (
      account.tenantId.toLowerCase() !==
      tenant.entraTenantId.toLowerCase()
    ) {
      throw new Error(
        'You signed in with an account from the wrong organisation.'
      );
    }

    const customerAuthority =
      `https://login.microsoftonline.com/${tenant.entraTenantId}`;

    const apiToken = await this.acquireApiToken(
      account,
      customerAuthority
    );

    const tokenClaims =
      this.decodeAccessTokenClaims(apiToken);

    this.validatePlatformApiClaims(
      tokenClaims,
      tenant.entraTenantId
    );

    this.statusMessage = 'Loading your organisation profile...';

    const context = await firstValueFrom(
      this.http.get<UserContextResponse>(
        `${environment.platformApi.baseUrl}/api/platform/v1/me/context`,
        {
          headers: new HttpHeaders({
            Authorization: `Bearer ${apiToken}`
          })
        }
      )
    );

    // Verify that the backend resolved the expected customer.
    if (context.tenantSlug !== tenant.tenantSlug) {
      throw new Error('Organisation context verification failed.');
    }

    if (!context.roles?.length) {
      throw new Error('No application roles are assigned.');
    }

    this.isLoading = false;

    if (context.roles.includes('COMPANY_ADMIN')) {

      this.statusMessage =
        `Welcome to ${context.companyName}`;

      await this.router.navigate([
        '/app',
        context.tenantSlug,
        'dashboard'
      ]);

      return;
    }

    // Add other customer roles here when implemented.
    if (context.roles.includes('COMPANY_USER')) {

      this.statusMessage =
        `Welcome to ${context.companyName}`;

      await this.router.navigate([
        '/app',
        context.tenantSlug,
        'home'
      ]);

      return;
    }

    throw new Error(
      'Your account does not have an authorised OmniTruth role.'
    );
  }

  // --------------------------------------------------
  // COMMON MICROSOFT LOGIN
  // --------------------------------------------------

  private startLogin(
    state: LoginState,
    authority: string
  ): void {

    this.isLoading = true;
    this.statusMessage = 'Redirecting to Microsoft...';
    this.errorMessage = '';

    const request: RedirectRequest = {

      authority,

      redirectUri: environment.azure.redirectUri,

      loginHint: this.email.trim() || undefined,

      scopes: [
        'openid',
        'profile',
        environment.platformApi.scope
      ],

      state: JSON.stringify(state)
    };

    this.msalService.loginRedirect(request).subscribe({

      error: (error) => {
        console.error('Microsoft login failed:', error);

        this.isLoading = false;
        this.statusMessage = '';
        this.errorMessage =
          'Unable to start Microsoft sign-in. Please try again.';
      }
    });
  }

  // --------------------------------------------------
  // API ACCESS TOKEN
  // --------------------------------------------------

  private async acquireApiToken(
    account: AccountInfo,
    authority: string
  ): Promise<string> {

    try {

      const result = await firstValueFrom(
        this.msalService.acquireTokenSilent({
          account,
          authority,
          scopes: [environment.platformApi.scope]
        })
      );

      return result.accessToken;

    } catch (error) {

      console.error('Silent token acquisition failed:', error);

      // Do not silently continue without an API access token.
      // Start a fresh authorization flow for this account.
      await firstValueFrom(
        this.msalService.acquireTokenRedirect({
          account,
          authority,
          redirectUri: environment.azure.redirectUri,
          scopes: [environment.platformApi.scope]
        })
      );

      throw error;
    }
  }

  // --------------------------------------------------
  // TOKEN DIAGNOSTICS
  // --------------------------------------------------

  private decodeAccessTokenClaims(
    accessToken: string
  ): PlatformApiTokenClaims {

    const parts = accessToken.split('.');

    if (parts.length !== 3) {
      throw new Error('Invalid JWT format.');
    }

    const base64 = parts[1]
      .replace(/-/g, '+')
      .replace(/_/g, '/');

    const padded = base64.padEnd(
      Math.ceil(base64.length / 4) * 4,
      '='
    );

    const decoded = atob(padded);

    const bytes = Uint8Array.from(
      decoded,
      char => char.charCodeAt(0)
    );

    return JSON.parse(
      new TextDecoder().decode(bytes)
    ) as PlatformApiTokenClaims;
  }

  private validatePlatformApiClaims(
    claims: PlatformApiTokenClaims,
    expectedTenantId: string
  ): void {

    if (claims.aud !== environment.platformApi.clientId) {
      throw new Error('Unexpected Platform API audience.');
    }

    if (
      claims.tid?.toLowerCase() !==
      expectedTenantId.toLowerCase()
    ) {
      throw new Error('Unexpected Entra tenant in access token.');
    }

    if (
      !claims.scp?.split(' ').includes('access_as_user')
    ) {
      throw new Error('Platform API delegated scope is missing.');
    }

    if (claims.exp && claims.exp * 1000 <= Date.now()) {
      throw new Error('Platform API access token has expired.');
    }

    console.log('Platform API token diagnostics:', {
      aud: claims.aud,
      tid: claims.tid,
      roles: claims.roles,
      scp: claims.scp,
      ver: claims.ver
    });
  }

  clearStatus(): void {
    this.statusMessage = '';
    this.errorMessage = '';
  }
}
