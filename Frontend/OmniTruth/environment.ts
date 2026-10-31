// Merge these properties into your existing src/environments/environment.ts.
// Do not replace other environment settings already used by your application.
export const environment = {
  production: false,
  platformApiBaseUrl: 'http://localhost:8081',
  platformApiScope: 'api://5e5f08a-019b-46bf-87e9-26d8148235ad/access_as_user',
  azure: {
    tenantId: 'eb39d9b2-f8de-495c-9ec7-3efcf7b4d9e5',
    clientId: '7747b071-0307-47ab-a4dc-5f4bae362a6d',
    authority: 'https://login.microsoftonline.com/eb39d9b2-f8de-495c-9ec7-3efcf7b4d9e5'
  },
  
  platformApi: {
    clientId: '85e5f08a-019b-46bf-87e9-26d8148235ad',
    baseUrl: 'http://localhost:8081',
    scope: 'api://5e5f08a-019b-46bf-87e9-26d8148235ad/access_as_user'
  }
};

