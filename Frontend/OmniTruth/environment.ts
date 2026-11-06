
const omniTruthApiScope =
  'api://0309a226-b400-4a88-a901-d09639b64f42/access_as_user';

export const environment = {
  production: false,

  azure: {
    // OmniTruth vendor Entra tenant
    tenantId: '30556ea2-dc45-48a0-8d9e-ad2b64c195dc',

    // Angular application registration
    clientId: '89855a3d-f952-46be-8034-677c6cef5ba7',

    // Vendor organizational account authority
    authority:
      'https://login.microsoftonline.com/30556ea2-dc45-48a0-8d9e-ad2b64c195dc',

    // Shared sign-in: personal Microsoft + organizational accounts
    commonAuthority:
      'https://login.microsoftonline.com/common',

    // Angular redirect URI
    redirectUri:
      'http://localhost:4200/login'
  },

  platformApi: {
    clientId:
      '0309a226-b400-4a88-a901-d09639b64f42',

    baseUrl:
      'http://localhost:8081',

    scope: omniTruthApiScope
  },

  tenantManagement: {
    baseUrl:
      'http://localhost:8082',

    scope: omniTruthApiScope
  },

  userManagement: {
    baseUrl:
      'http://localhost:8083',

    scope: omniTruthApiScope
  }
};
