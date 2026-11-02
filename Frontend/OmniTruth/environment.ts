
export const environment = {
  production: false,

  azure: {
    tenantId: '30556ea2-dc45-48a0-8d9e-ad2b64c195dc',
    clientId: '89855a3d-f952-46be-8034-677c6cef5ba7',

    authority:
      'https://login.microsoftonline.com/30556ea2-dc45-48a0-8d9e-ad2b64c195dc',

    organizationsAuthority:
      'https://login.microsoftonline.com/organizations',

    redirectUri: 'http://localhost:4200/login'
  },

  platformApi: {
    clientId: '0309a226-b400-4a88-a901-d09639b64f42',
    baseUrl: 'http://localhost:8081',
    scope:
      'api://0309a226-b400-4a88-a901-d09639b64f42/access_as_user'
  },

  tenantManagement: {
    baseUrl: 'http://localhost:8082'
  }
};
