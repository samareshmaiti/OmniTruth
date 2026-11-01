
export const environment = {
  production: false,

  azure: {
    tenantId: 'eb39d9b2-f8de-495c-9ec7-3efcf7b4d9e5',
    clientId: '7747b071-0307-47ab-a4dc-5f4bae362a6d',
    authority:
      'https://login.microsoftonline.com/eb39d9b2-f8de-495c-9ec7-3efcf7b4d9e5'
  },

  platformApi: {
    clientId: '85e5f08a-019b-46bf-87e9-26d8148235ad',
    baseUrl: 'http://localhost:8081',

    // Must exactly match the scope exposed by your API registration.
    scope:
      'api://85e5f08a-019b-46bf-87e9-26d8148235ad/access_as_user'
  }
};
