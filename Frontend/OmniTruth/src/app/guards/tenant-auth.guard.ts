import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { MsalService } from '@azure/msal-angular';
import { TenantContextService } from '../services/tenant-context.service';

/**
 * Use after MsalGuard. MsalGuard checks the Microsoft session; this guard
 * checks the User Management membership and ensures the URL tenant matches
 * the tenant returned by the backend.
 */
export const tenantAuthGuard: CanActivateFn = async (route, state) => {
  const msal = inject(MsalService);
  const tenantContext = inject(TenantContextService);
  const router = inject(Router);

  const account = msal.instance.getActiveAccount()
    ?? msal.instance.getAllAccounts()[0]
    ?? null;

  if (!account) {
    return router.createUrlTree(['/login'], {
      queryParams: { returnUrl: state.url }
    });
  }

  try {
    const context = await tenantContext.getMyContext();
    const requestedSlug = route.paramMap.get('tenantSlug');

    if (context.accessStatus !== 'ACTIVE') {
      return router.createUrlTree(['/login'], {
        queryParams: { access: 'membership-inactive' }
      });
    }

    if (!context.tenantSlug ||
        context.tenantSlug.toLowerCase() !== (requestedSlug ?? '').toLowerCase()) {
      return router.createUrlTree(['/error']);
    }

    const allowedRoles = (route.data['roles'] as string[] | undefined)
      ?? ['COMPANY_ADMIN'];

    if (!context.roles?.some(role => allowedRoles.includes(role))) {
      return router.createUrlTree(['/error']);
    }

    return true;
  } catch {
    return router.createUrlTree(['/login'], {
      queryParams: { access: 'context-unavailable' }
    });
  }
};
