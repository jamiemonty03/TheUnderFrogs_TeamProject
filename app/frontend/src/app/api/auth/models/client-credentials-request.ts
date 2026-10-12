export interface ClientCredentialsRequest {

  client_id: string;

  client_secret: string;

  grant_type: 'client_credentials';
}
