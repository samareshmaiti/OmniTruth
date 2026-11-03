import { CommonModule } from '@angular/common';
import { Component, OnInit, inject } from '@angular/core';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { firstValueFrom } from 'rxjs';
import {
  InvitationDetails,
  TenantOnboardingService
} from '../../../services/tenant-onboarding.service';

@Component({
  selector: 'app-invitation-acceptance',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './invitation-acceptance.component.html',
  styleUrl: './invitation-acceptance.component.css'
})
export class InvitationAcceptanceComponent implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly onboarding = inject(TenantOnboardingService);

  invitation: InvitationDetails | null = null;
  invitationToken = '';
  loading = true;
  accepting = false;
  error = '';
  notice = '';

  async ngOnInit(): Promise<void> {
    const token = this.route.snapshot.queryParamMap.get('token')
      ?? this.onboarding.getPendingInvitationToken();

    if (!token) {
      this.loading = false;
      this.error = 'This invitation link is missing its token. Please use the link from your invitation email.';
      return;
    }

    this.invitationToken = token;
    this.onboarding.storePendingInvitationToken(token);

    // Remove the one-time token from the visible URL after reading it.
    if (this.route.snapshot.queryParamMap.has('token')) {
      await this.router.navigate([], {
        relativeTo: this.route,
        queryParams: {},
        replaceUrl: true
      });
    }

    try {
      this.invitation = await firstValueFrom(
        this.onboarding.validateInvitation(token)
      );

      if (this.invitation.status === 'EXPIRED' || this.invitation.status === 'REVOKED') {
        this.error = `This invitation is ${this.invitation.status.toLowerCase()}. Please ask your OmniTruth administrator for a new invitation.`;
      } else if (this.invitation.status === 'ACCEPTED') {
        this.notice = 'This invitation has already been completed. You can sign in to your organisation.';
      } else if (this.invitation.status === 'ACCEPTED_PENDING_SSO') {
        this.notice = 'Your invitation has been accepted. Continue with Microsoft authorization.';
      }
    } catch (error: any) {
      this.onboarding.clearPendingInvitationToken();
      this.error = error?.status === 410
        ? 'This invitation has expired. Please request a new invitation.'
        : 'We could not validate this invitation. It may be invalid, expired or already used.';
    } finally {
      this.loading = false;
    }
  }

  async acceptInvitation(): Promise<void> {
    if (!this.invitationToken || this.accepting) return;

    this.accepting = true;
    this.error = '';
    this.notice = '';

    try {
      this.invitation = await firstValueFrom(
        this.onboarding.acceptInvitation(this.invitationToken)
      );
      this.notice = 'Invitation accepted. Continue to Microsoft SSO authorization.';
      await this.router.navigate(['/onboarding/entra']);
    } catch (error: any) {
      this.error = error?.status === 410
        ? 'This invitation has expired. Please request a new invitation.'
        : 'We could not accept this invitation. Please reload the page and try again.';
    } finally {
      this.accepting = false;
    }
  }

  continueToSso(): void {
    this.router.navigate(['/onboarding/entra']);
  }

  get canAccept(): boolean {
    return this.invitation?.status === 'PENDING';
  }

  get canContinue(): boolean {
    return this.invitation?.status === 'ACCEPTED_PENDING_SSO';
  }
}
