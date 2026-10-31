import { CommonModule } from '@angular/common';
import { Component, OnInit, inject } from '@angular/core';
import { FormsModule, NgForm } from '@angular/forms';
import { MsalService } from '@azure/msal-angular';
import { AccountInfo } from '@azure/msal-browser';
import { ApiServices, DashboardSummary, Tenant, TenantStatus } from '../../../../../apiservices';

 type AdminView = 'overview' | 'tenants';
type ModalMode = 'create' | 'invite' | 'details' | null;

@Component({
  selector: 'app-superadmin',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './superadmin.component.html',
  styleUrls: ['./superadmin.component.css']
})
export class SuperadminComponent implements OnInit {
  private readonly api = inject(ApiServices);
  private readonly msal = inject(MsalService);

  readonly currentYear = new Date().getFullYear();
  readonly pageSize = 10;
  readonly statuses: TenantStatus[] = [
    'PENDING',
    'SSO_CONFIGURING',
    'PROVISIONING',
    'ACTIVE',
    'SUSPENDED',
    'CLOSED'
  ];

  activeView: AdminView = 'overview';
  activeAccount: AccountInfo | null = null;
  profileName = 'Platform Administrator';
  profileEmail = '';
  mobileNavOpen = false;

  tenants: Tenant[] = [];
  summary: DashboardSummary = this.emptySummary();
  totalElements = 0;
  totalPages = 0;
  currentPage = 0;
  selectedStatus: TenantStatus | '' = '';
  searchTerm = '';

  loadingTenants = false;
  loadingSummary = false;
  loadingDetails = false;
  saving = false;
  backendHealthy = false;

  pageError = '';
  modalError = '';
  toastMessage = '';
  toastType: 'success' | 'error' = 'success';
  private toastTimer?: number;

  activeModal: ModalMode = null;
  createForm = { companyName: '', tenantCode: '', industry: '' };
  inviteTenant: Tenant | null = null;
  inviteEmail = '';
  detailTenant: Tenant | null = null;

  ngOnInit(): void {
    this.activeAccount = this.msal.instance.getActiveAccount() ??
      this.msal.instance.getAllAccounts()[0] ?? null;

    if (this.activeAccount && !this.msal.instance.getActiveAccount()) {
      this.msal.instance.setActiveAccount(this.activeAccount);
    }

    if (this.activeAccount) {
      this.profileName = this.activeAccount.name || this.activeAccount.username;
      this.profileEmail = this.activeAccount.username;
      this.refreshAll();
    } else {
      this.pageError = 'No signed-in Microsoft account was found. Complete Microsoft sign-in before opening the Super Admin dashboard.';
    }
  }

  get firstName(): string {
    const display = this.profileName || 'Administrator';
    return display.trim().split(/\s+/)[0] || 'Administrator';
  }

  get profileInitials(): string {
    const parts = (this.profileName || 'PA').trim().split(/\s+/).filter(Boolean);
    return parts.slice(0, 2).map(part => part.charAt(0).toUpperCase()).join('') || 'PA';
  }

  get filteredTenants(): Tenant[] {
    const term = this.searchTerm.trim().toLowerCase();
    if (!term) return this.tenants;

    return this.tenants.filter(tenant =>
      tenant.companyName.toLowerCase().includes(term) ||
      tenant.tenantCode.toLowerCase().includes(term) ||
      tenant.tenantId.toLowerCase().includes(term) ||
      (tenant.industry || '').toLowerCase().includes(term)
    );
  }

  get pageStart(): number {
    return this.totalElements === 0 ? 0 : this.currentPage * this.pageSize + 1;
  }

  get pageEnd(): number {
    return Math.min((this.currentPage + 1) * this.pageSize, this.totalElements);
  }

  setView(view: AdminView): void {
    this.activeView = view;
    this.mobileNavOpen = false;
    if (view === 'tenants' && this.tenants.length === 0 && !this.loadingTenants) {
      this.loadTenants();
    }
  }

  refreshAll(): void {
    this.pageError = '';
    this.loadTenants(0);
    this.loadSummary();
    this.checkBackendHealth();
  }

  loadTenants(page = this.currentPage): void {
    this.loadingTenants = true;
    this.api.getTenants(this.selectedStatus, page, this.pageSize).subscribe({
      next: result => {
        this.tenants = result.content || [];
        this.totalElements = result.totalElements ?? this.tenants.length;
        this.totalPages = result.totalPages ?? 1;
        this.currentPage = result.number ?? page;
        this.loadingTenants = false;
      },
      error: error => {
        this.loadingTenants = false;
        this.pageError = this.errorMessage(error, 'Unable to load tenant records.');
      }
    });
  }

  private loadSummary(): void {
    this.loadingSummary = true;
    this.api.getDashboardSummary().subscribe({
      next: result => {
        this.summary = result;
        this.loadingSummary = false;
      },
      error: error => {
        this.loadingSummary = false;
        this.pageError = this.errorMessage(error, 'Unable to load tenant summary.');
      }
    });
  }

  private checkBackendHealth(): void {
    this.api.getBackendHealth().subscribe({
      next: result => this.backendHealthy = result?.status?.toUpperCase() === 'UP',
      error: () => this.backendHealthy = false
    });
  }

  onStatusFilterChange(status: TenantStatus | ''): void {
    this.selectedStatus = status;
    this.currentPage = 0;
    this.searchTerm = '';
    this.loadTenants(0);
  }

  goToPage(page: number): void {
    if (page < 0 || page >= this.totalPages || this.loadingTenants) return;
    this.loadTenants(page);
  }

  openCreateTenant(): void {
    this.createForm = { companyName: '', tenantCode: '', industry: '' };
    this.modalError = '';
    this.activeModal = 'create';
  }

  createTenant(form: NgForm): void {
    if (form.invalid || this.saving) {
      form.control.markAllAsTouched();
      return;
    }

    const payload = {
      companyName: this.createForm.companyName.trim(),
      tenantCode: this.createForm.tenantCode.trim(),
      industry: this.createForm.industry.trim() || undefined
    };

    if (!payload.companyName || !payload.tenantCode) {
      this.modalError = 'Company name and tenant code are required.';
      return;
    }

    this.saving = true;
    this.modalError = '';
    this.api.createTenant(payload).subscribe({
      next: created => {
        this.saving = false;
        this.closeModal();
        this.selectedStatus = '';
        this.searchTerm = '';
        this.currentPage = 0;
        this.loadTenants(0);
        this.loadSummary();
        this.showToast(`${created.companyName} was registered successfully.`);
      },
      error: error => {
        this.saving = false;
        this.modalError = this.errorMessage(error, 'Tenant could not be created.');
      }
    });
  }

  viewTenant(tenant: Tenant): void {
    this.detailTenant = tenant;
    this.loadingDetails = true;
    this.modalError = '';
    this.activeModal = 'details';

    this.api.getTenant(tenant.tenantId).subscribe({
      next: result => {
        this.detailTenant = result;
        this.loadingDetails = false;
      },
      error: error => {
        this.loadingDetails = false;
        this.modalError = this.errorMessage(error, 'Unable to retrieve tenant details.');
        this.showToast(this.modalError, 'error');
      }
    });
  }

  allowedNextStatuses(current: TenantStatus): TenantStatus[] {
    const transitions: Record<TenantStatus, TenantStatus[]> = {
      PENDING: ['SSO_CONFIGURING', 'SUSPENDED', 'CLOSED'],
      SSO_CONFIGURING: ['PROVISIONING', 'SUSPENDED', 'CLOSED'],
      PROVISIONING: ['ACTIVE', 'SUSPENDED', 'CLOSED'],
      ACTIVE: ['SUSPENDED', 'CLOSED'],
      SUSPENDED: ['ACTIVE', 'CLOSED'],
      CLOSED: []
    };
    return transitions[current];
  }

  changeTenantStatus(tenant: Tenant, nextStatus: TenantStatus): void {
    if (nextStatus === tenant.status) return;

    const allowed = this.allowedNextStatuses(tenant.status);
    if (!allowed.includes(nextStatus)) {
      this.showToast(`The transition ${this.statusLabel(tenant.status)} → ${this.statusLabel(nextStatus)} is not allowed.`, 'error');
      return;
    }

    const confirmed = window.confirm(
      `Change ${tenant.companyName} from ${this.statusLabel(tenant.status)} to ${this.statusLabel(nextStatus)}?`
    );
    if (!confirmed) return;

    this.api.updateTenantStatus(tenant.tenantId, nextStatus).subscribe({
      next: updated => {
        this.replaceTenant(updated);
        if (this.detailTenant?.tenantId === updated.tenantId) this.detailTenant = updated;
        this.loadSummary();
        this.showToast(`${updated.companyName} is now ${this.statusLabel(updated.status)}.`);
      },
      error: error => this.showToast(this.errorMessage(error, 'Tenant status could not be updated.'), 'error')
    });
  }

  openInviteAdmin(tenant: Tenant | null): void {
    if (!tenant) return;
    if (tenant.status !== 'SSO_CONFIGURING') {
      this.showToast('The first Company Admin can only be invited while the tenant is in SSO Configuring.', 'error');
      return;
    }
    this.inviteTenant = tenant;
    this.inviteEmail = '';
    this.modalError = '';
    this.activeModal = 'invite';
  }

  inviteCompanyAdmin(form: NgForm): void {
    if (!this.inviteTenant || form.invalid || this.saving) {
      form.control.markAllAsTouched();
      return;
    }

    this.saving = true;
    this.modalError = '';
    this.api.inviteCompanyAdmin(this.inviteTenant.tenantId, this.inviteEmail.trim()).subscribe({
      next: result => {
        this.saving = false;
        this.closeModal();
        this.showToast(result.message || `Invitation created for ${result.invitedEmail}.`);
      },
      error: error => {
        this.saving = false;
        this.modalError = this.errorMessage(error, 'Company Admin invitation could not be created.');
      }
    });
  }

  closeModal(): void {
    this.activeModal = null;
    this.modalError = '';
    this.inviteTenant = null;
    this.detailTenant = null;
    this.loadingDetails = false;
  }

  statusLabel(status: TenantStatus): string {
    return status.replace(/_/g, ' ').toLowerCase().replace(/\b\w/g, char => char.toUpperCase());
  }

  statusClass(status: TenantStatus): string {
    return `status-${status.toLowerCase().replace(/_/g, '-')}`;
  }

  companyInitials(companyName: string): string {
    return companyName.trim().split(/\s+/).filter(Boolean).slice(0, 2)
      .map(part => part.charAt(0).toUpperCase()).join('') || 'CO';
  }

  lifecyclePercent(count: number): number {
    if (!this.summary.totalTenants) return 0;
    return Math.min(100, (count / this.summary.totalTenants) * 100);
  }

  trackTenant(_index: number, tenant: Tenant): string {
    return tenant.tenantId;
  }

  logout(): void {
    this.msal.logoutRedirect({ account: this.activeAccount ?? undefined }).subscribe({
      error: error => this.showToast(this.errorMessage(error, 'Microsoft sign-out could not be started.'), 'error')
    });
  }

  private replaceTenant(updated: Tenant): void {
    this.tenants = this.tenants.map(tenant =>
      tenant.tenantId === updated.tenantId ? updated : tenant
    );
  }

  private showToast(message: string, type: 'success' | 'error' = 'success'): void {
    this.toastMessage = message;
    this.toastType = type;
    if (this.toastTimer) window.clearTimeout(this.toastTimer);
    this.toastTimer = window.setTimeout(() => this.toastMessage = '', 5000);
  }

  private errorMessage(error: unknown, fallback: string): string {
    const candidate = error as {
      error?: { message?: string; detail?: string; title?: string };
      message?: string;
    };
    return candidate?.error?.message || candidate?.error?.detail ||
      candidate?.message || fallback;
  }

  private emptySummary(): DashboardSummary {
    return {
      totalTenants: 0,
      pending: 0,
      ssoConfiguring: 0,
      provisioning: 0,
      active: 0,
      suspended: 0,
      closed: 0,
      onboarding: 0
    };
  }
}
