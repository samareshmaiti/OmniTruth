import { CommonModule } from '@angular/common';
import { Component, OnInit, inject } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { MsalService } from '@azure/msal-angular';
import { AccountInfo, AuthenticationResult, RedirectRequest } from '@azure/msal-browser';
import { firstValueFrom } from 'rxjs';
import { environment } from '../../../../../environment';
import { TenantContextService ,TenantBootstrapContext} from '../../../services/tenant-context.service';
import { TenantOnboardingService } from '../../../services/tenant-onboarding.service';

@Component({
  selector: 'app-entra-authorization',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './entra-authorization.component.html',
  styleUrl: './entra-authorization.component.css'
})
export class EntraAuthorizationComponent implements OnInit {
  private readonly msal = inject(MsalService);
  private readonly onboarding = inject(TenantOnboardingService);
  private readonly tenantContext = inject(TenantContextService);
  private readonly router = inject(Router);

  loading = false;
  authorizing = false;
  error = '';
  notice = '';
  invitationToken = '';
  signedInEmail = '';
  provisioning = false;
  private completionStarted = false;

  ngOnInit(): void {
    this.invitationToken = this.onboarding.getPendingInvitationToken() ?? '';

    if (!this.invitationToken) {
      this.error = 'Your invitation session is missing. Please reopen the invitation link from your email.';
      return;
    }

    // This route is also the redirect URI for the first Company Admin SSO flow.
    this.msal.handleRedirectObservable().subscribe({
      next: result => {
        if (result?.account) {
          void this.finishMicrosoftSignIn(result);
        }
      },
      error: error => {
        console.error('Entra redirect processing failed:', error);
        this.authorizing = false;
        this.error = this.microsoftError(error);
      }
    });
  }


  startMicrosoftAuthorization(): void {
    if (this.authorizing || !this.invitationToken) return;
  
    const tenantScope = environment.tenantManagement.scope;
  
    if (!tenantScope || tenantScope.includes('YOUR-')) {
      this.error = 'Tenant Management API scope is not configured.';
      return;
    }
  
    this.authorizing = true;
    this.error = '';
    this.notice = 'Redirecting to your organisation’s Microsoft sign-in…';
  
    const request: RedirectRequest = {
      authority: 'https://login.microsoftonline.com/organizations',
  
      redirectUri: 'http://localhost:4200/onboarding/entra',
  
      prompt: 'select_account',
  
      scopes: [
        'openid',
        'profile',
        tenantScope
      ]
    };
  
    console.log('Company Admin authentication authority:',
      request.authority);
  
    console.log('Requested API scope:', tenantScope);
  
    this.msal.loginRedirect(request).subscribe({
      error: error => {
        console.error('Microsoft authorization failed:', error);
        this.authorizing = false;
        this.error = this.microsoftError(error);
      }
    });
  }
  

  async checkProvisioningAgain(): Promise<void> {
    if (this.provisioning) return;
    this.provisioning = true;
    this.error = '';
    this.notice = 'Checking whether your tenant membership is active…';

    try {
      const context = await this.tenantContext.waitForCompanyAdminAccess(10, 2000);
      await this.openWorkspace(context);
    } catch (error: any) {
      this.error = error?.message ?? 'Your membership is not active yet.';
    } finally {
      this.provisioning = false;
    }
  }

  private async finishMicrosoftSignIn(result: AuthenticationResult): Promise<void> {
    if (this.completionStarted) return;
    this.completionStarted = true;

    const account: AccountInfo = result.account;
    this.msal.instance.setActiveAccount(account);
    this.signedInEmail = account.username;
    this.loading = true;
    this.authorizing = false;
    this.error = '';
    this.notice = 'Verifying your Microsoft identity and completing tenant onboarding…';

    try {
      const completion = await this.onboarding.completeInvitation(
        this.invitationToken,
        account
      );

      // Tenant Management has accepted the Entra identity and published the
      // provisioning event. User Management now creates the membership.
      this.onboarding.clearPendingInvitationToken();
      this.invitationToken = '';
      this.notice = completion.message || 'Identity verified. Waiting for your workspace to become ready…';

      await this.checkAndOpenWorkspace();
    } catch (error: any) {
      this.completionStarted = false;
      this.error = this.microsoftError(error);
    } finally {
      this.loading = false;
    }
  }

  private async checkAndOpenWorkspace(): Promise<void> {
    this.provisioning = true;

    try {
      const context = await this.tenantContext.waitForCompanyAdminAccess(45, 2000);
      await this.openWorkspace(context);
    } catch (error: any) {
      this.error = error?.message ?? 'Your account is still being provisioned.';
      if (String(error?.message ?? '').includes('does not have the Company Admin role')) {
        this.notice = 'Your membership is active, but the expected Company Admin role was not returned by User Management.';
      } else {
        this.notice = 'Your Microsoft identity was verified. User Management may still be processing the Kafka onboarding event. You can check again below.';
      }
    } finally {
      this.provisioning = false;
    }
  }

  private async openWorkspace(context: TenantBootstrapContext): Promise<void> {
    if (!context.roles?.includes('COMPANY_ADMIN')) {
      throw new Error('Your account is active, but it does not have the Company Admin role. Please contact your organisation administrator.');
    }

    this.notice = `Welcome to ${context.companyName}. Opening your workspace…`;
    await this.router.navigate([
      '/app',
      context.tenantSlug,
      'dashboard'
    ]);
  }

  private microsoftError(error: any): string {
    const code = error?.errorCode ?? error?.code ?? '';
    const message = String(error?.message ?? '').toLowerCase();

    if (code === 'consent_required' || code === 'interaction_required' ||
        message.includes('aadsts65001') || message.includes('consent')) {
      return 'Your organisation has not yet granted the required permission to OmniTruth. Ask an Entra administrator to approve the OmniTruth application, then try again.';
    }

    if (error?.status === 410) {
      return 'This invitation has expired. Ask your OmniTruth administrator for a new invitation.';
    }

    return 'Microsoft authorization or onboarding could not be completed. Please try again or contact your OmniTruth administrator.';
  }
}
