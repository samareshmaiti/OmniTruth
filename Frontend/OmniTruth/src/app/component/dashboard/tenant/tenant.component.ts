import { CommonModule } from '@angular/common';
import { Component, OnInit, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute } from '@angular/router';
import { MsalService } from '@azure/msal-angular';
import {
  TenantManagementApiService, TenantDashboard, TenantClient,
  TenantClientRequest, TenantApiEndpoint, TenantApiEndpointRequest,
  TenantTool, TenantToolRequest, TenantProfile, TenantProfileRequest
} from '../../../services/tenant-management-api.service';

type TenantView = 'overview' | 'profile' | 'clients' | 'endpoints' | 'tools';

@Component({
  selector: 'app-tenant',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './tenant.component.html',
  styleUrl: './tenant.component.css'
})
export class TenantComponent implements OnInit {
  private readonly api = inject(TenantManagementApiService);
  private readonly route = inject(ActivatedRoute);
  private readonly msal = inject(MsalService);

  readonly currentYear = new Date().getFullYear();
  tenantSlug = '';
  companyName = 'Company workspace';
  profileEmail = '';
  profileLoading = false;
  tenantProfile: TenantProfile | null = null;
  profileForm: TenantProfileRequest = { companyName: '', industry: '' };
  activeView: TenantView = 'overview';
  loading = true;
  saving = false;
  error = '';
  notice = '';

  dashboard: TenantDashboard | null = null;
  clients: TenantClient[] = [];
  endpoints: TenantApiEndpoint[] = [];
  tools: TenantTool[] = [];

  editingClientId: string | null = null;
  clientForm: TenantClientRequest = this.emptyClient();

  editingEndpointId: string | null = null;
  endpointForm: TenantApiEndpointRequest = this.emptyEndpoint();

  editingToolId: string | null = null;
  toolForm: TenantToolRequest = this.emptyTool();

  ngOnInit(): void {
    this.tenantSlug = this.route.snapshot.paramMap.get('tenantSlug') ?? '';
    const account = this.msal.instance.getActiveAccount() ?? this.msal.instance.getAllAccounts()[0];
    this.profileEmail = account?.username ?? '';
    this.loadWorkspace();
  }

  get pageTitle(): string {
    return ({
      overview: 'Dashboard',
      profile: 'Company profile',
      clients: 'Client management',
      endpoints: 'API endpoints',
      tools: 'Tool calling'
    } as Record<TenantView, string>)[this.activeView];
  }

  setView(view: TenantView): void {
    this.activeView = view;
    this.error = '';
    this.notice = '';
    if (view === 'profile') this.loadProfile();
  }

  loadProfile(): void {
    this.profileLoading = true;
    this.api.profile().subscribe({
      next: profile => {
        this.tenantProfile = profile;
        this.companyName = profile.companyName;
        this.profileForm = {
          companyName: profile.companyName,
          industry: profile.industry ?? ''
        };
        this.profileLoading = false;
      },
      error: err => {
        this.profileLoading = false;
        this.error = this.errorText(err, 'Unable to load company profile.');
      }
    });
  }

  saveProfile(): void {
    const companyName = this.profileForm.companyName.trim();
    if (!companyName || this.saving) return;

    this.saving = true;
    this.error = '';
    const body: TenantProfileRequest = {
      companyName,
      industry: this.profileForm.industry?.trim() || null
    };

    this.api.updateProfile(body).subscribe({
      next: profile => {
        this.tenantProfile = profile;
        this.companyName = profile.companyName;
        this.profileForm = {
          companyName: profile.companyName,
          industry: profile.industry ?? ''
        };
        if (this.dashboard) {
          this.dashboard.tenant.companyName = profile.companyName;
        }
        this.saving = false;
        this.notice = 'Company profile updated successfully.';
      },
      error: err => {
        this.saving = false;
        this.error = this.errorText(err, 'Unable to update company profile.');
      }
    });
  }

  loadWorkspace(): void {
    this.loading = true;
    this.error = '';
    this.api.dashboard().subscribe({
      next: value => {
        this.dashboard = value;
        this.companyName = value.tenant.companyName;
        this.loadLists();
      },
      error: err => {
        this.loading = false;
        this.error = this.errorText(err, 'Unable to load your tenant dashboard.');
      }
    });
  }

  private loadLists(): void {
    this.api.clients().subscribe({
      next: rows => this.clients = rows,
      error: err => this.error = this.errorText(err, 'Unable to load clients.')
    });
    this.api.endpoints().subscribe({
      next: rows => this.endpoints = rows,
      error: err => this.error = this.errorText(err, 'Unable to load API endpoints.')
    });
    this.api.tools().subscribe({
      next: rows => {
        this.tools = rows;
        this.loading = false;
      },
      error: err => {
        this.loading = false;
        this.error = this.errorText(err, 'Unable to load tool definitions.');
      }
    });
  }

  emptyClient(): TenantClientRequest {
    return { clientName: '', contactEmail: '', externalReference: '', notes: '', status: 'ACTIVE' };
  }

  startNewClient(): void {
    this.editingClientId = null;
    this.clientForm = this.emptyClient();
    this.setView('clients');
  }

  editClient(client: TenantClient): void {
    this.editingClientId = client.clientId;
    this.clientForm = {
      clientName: client.clientName,
      contactEmail: client.contactEmail ?? '',
      externalReference: client.externalReference ?? '',
      notes: client.notes ?? '',
      status: client.status
    };
  }

  saveClient(): void {
    if (!this.clientForm.clientName.trim() || this.saving) return;
    this.saving = true;
    const body = { ...this.clientForm, clientName: this.clientForm.clientName.trim() };
    const request = this.editingClientId
      ? this.api.updateClient(this.editingClientId, body)
      : this.api.createClient(body);
    request.subscribe({
      next: () => {
        this.saving = false;
        this.notice = this.editingClientId ? 'Client updated.' : 'Client added.';
        this.editingClientId = null;
        this.clientForm = this.emptyClient();
        this.refresh();
      },
      error: err => {
        this.saving = false;
        this.error = this.errorText(err, 'Unable to save client.');
      }
    });
  }

  emptyEndpoint(): TenantApiEndpointRequest {
    return {
      endpointName: '', baseUrl: '', httpMethod: 'GET', authType: 'NONE',
      secretReference: '', description: '', enabled: true
    };
  }

  startNewEndpoint(): void {
    this.editingEndpointId = null;
    this.endpointForm = this.emptyEndpoint();
    this.setView('endpoints');
  }

  editEndpoint(endpoint: TenantApiEndpoint): void {
    this.editingEndpointId = endpoint.endpointId;
    this.endpointForm = {
      endpointName: endpoint.endpointName,
      baseUrl: endpoint.baseUrl,
      httpMethod: endpoint.httpMethod,
      authType: endpoint.authType,
      secretReference: endpoint.secretReference ?? '',
      description: endpoint.description ?? '',
      enabled: endpoint.enabled
    };
  }

  saveEndpoint(): void {
    if (!this.endpointForm.endpointName.trim() || !this.endpointForm.baseUrl.trim() || this.saving) return;
    this.saving = true;
    const request = this.editingEndpointId
      ? this.api.updateEndpoint(this.editingEndpointId, this.endpointForm)
      : this.api.createEndpoint(this.endpointForm);
    request.subscribe({
      next: () => {
        this.saving = false;
        this.notice = this.editingEndpointId ? 'API endpoint updated.' : 'API endpoint added.';
        this.editingEndpointId = null;
        this.endpointForm = this.emptyEndpoint();
        this.refresh();
      },
      error: err => {
        this.saving = false;
        this.error = this.errorText(err, 'Unable to save API endpoint.');
      }
    });
  }

  emptyTool(): TenantToolRequest {
    return {
      toolName: '', description: '', endpointId: '',
      inputSchema: '{\n  "type": "object",\n  "properties": {}\n}', enabled: true
    };
  }

  startNewTool(): void {
    this.editingToolId = null;
    this.toolForm = this.emptyTool();
    this.setView('tools');
  }

  editTool(tool: TenantTool): void {
    this.editingToolId = tool.toolId;
    this.toolForm = {
      toolName: tool.toolName,
      description: tool.description,
      endpointId: tool.endpointId,
      inputSchema: tool.inputSchema,
      enabled: tool.enabled
    };
  }

  saveTool(): void {
    if (!this.toolForm.toolName.trim() || !this.toolForm.endpointId || this.saving) return;
    try {
      const parsed = JSON.parse(this.toolForm.inputSchema);
      if (!parsed || Array.isArray(parsed) || typeof parsed !== 'object') throw new Error();
    } catch {
      this.error = 'Input schema must be a valid JSON object.';
      return;
    }
    this.saving = true;
    const request = this.editingToolId
      ? this.api.updateTool(this.editingToolId, this.toolForm)
      : this.api.createTool(this.toolForm);
    request.subscribe({
      next: () => {
        this.saving = false;
        this.notice = this.editingToolId ? 'Tool definition updated.' : 'Tool definition added.';
        this.editingToolId = null;
        this.toolForm = this.emptyTool();
        this.refresh();
      },
      error: err => {
        this.saving = false;
        this.error = this.errorText(err, 'Unable to save tool definition.');
      }
    });
  }

  endpointName(id: string): string {
    return this.endpoints.find(endpoint => endpoint.endpointId === id)?.endpointName ?? id;
  }

  private refresh(): void {
    this.loadWorkspace();
  }

  logout(): void {
    this.msal.logoutRedirect({ postLogoutRedirectUri: window.location.origin + '/login' });
  }

  private errorText(error: any, fallback: string): string {
    if (error?.status === 401) return 'Your session is not authorised for Tenant Management. Please sign in again.';
    if (error?.status === 403) return 'Access denied. Your account must have the COMPANY_ADMIN role and an active tenant mapping.';
    return error?.error?.detail || error?.error?.message || error?.message || fallback;
  }
}
