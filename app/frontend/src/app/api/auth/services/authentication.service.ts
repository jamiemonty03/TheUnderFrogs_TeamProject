import { HttpClient, HttpContext } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';

import { BaseService } from '../base-service';
import { ApiConfiguration } from '../api-configuration';
import { StrictHttpResponse } from '../strict-http-response';

import { AuthTokens } from '../models/auth-tokens';
import { clientCredentials } from '../fn/authentication/client-credentials';
import { ClientCredentials$Params } from '../fn/authentication/client-credentials';
import { clientCredentials$XWwwFormUrlencoded } from '../fn/authentication/client-credentials-x-www-form-urlencoded';
import { ClientCredentials$XWwwFormUrlencoded$Params } from '../fn/authentication/client-credentials-x-www-form-urlencoded';
import { login } from '../fn/authentication/login';
import { Login$Params } from '../fn/authentication/login';
import { logout } from '../fn/authentication/logout';
import { Logout$Params } from '../fn/authentication/logout';
import { refresh } from '../fn/authentication/refresh';
import { Refresh$Params } from '../fn/authentication/refresh';
import { register } from '../fn/authentication/register';
import { Register$Params } from '../fn/authentication/register';
import { RegisterResponse } from '../models/register-response';
import { TokenResponse } from '../models/token-response';

@Injectable({ providedIn: 'root' })
export class AuthenticationService extends BaseService {
  constructor(config: ApiConfiguration, http: HttpClient) {
    super(config, http);
  }

  static readonly RegisterPath = '/auth/register';

  register$Response(params: Register$Params, context?: HttpContext): Observable<StrictHttpResponse<RegisterResponse>> {
    const obs = register(this.http, this.rootUrl, params, context);
    return obs;
  }

  register(params: Register$Params, context?: HttpContext): Observable<RegisterResponse> {
    const resp = this.register$Response(params, context);
    return resp.pipe(
      map((r: StrictHttpResponse<RegisterResponse>): RegisterResponse => r.body)
    );
  }

  static readonly LoginPath = '/auth/login';

  login$Response(params: Login$Params, context?: HttpContext): Observable<StrictHttpResponse<AuthTokens>> {
    const obs = login(this.http, this.rootUrl, params, context);
    return obs;
  }

  login(params: Login$Params, context?: HttpContext): Observable<AuthTokens> {
    const resp = this.login$Response(params, context);
    return resp.pipe(
      map((r: StrictHttpResponse<AuthTokens>): AuthTokens => r.body)
    );
  }

  static readonly RefreshPath = '/auth/refresh';

  refresh$Response(params: Refresh$Params, context?: HttpContext): Observable<StrictHttpResponse<AuthTokens>> {
    const obs = refresh(this.http, this.rootUrl, params, context);
    return obs;
  }

  refresh(params: Refresh$Params, context?: HttpContext): Observable<AuthTokens> {
    const resp = this.refresh$Response(params, context);
    return resp.pipe(
      map((r: StrictHttpResponse<AuthTokens>): AuthTokens => r.body)
    );
  }

  static readonly LogoutPath = '/auth/logout';

  logout$Response(params: Logout$Params, context?: HttpContext): Observable<StrictHttpResponse<void>> {
    const obs = logout(this.http, this.rootUrl, params, context);
    return obs;
  }

  logout(params: Logout$Params, context?: HttpContext): Observable<void> {
    const resp = this.logout$Response(params, context);
    return resp.pipe(
      map((r: StrictHttpResponse<void>): void => r.body)
    );
  }

  static readonly ClientCredentialsPath = '/auth/token';

  clientCredentials$Response(params: ClientCredentials$Params, context?: HttpContext): Observable<StrictHttpResponse<TokenResponse>> {
    const obs = clientCredentials(this.http, this.rootUrl, params, context);
    return obs;
  }

  clientCredentials(params: ClientCredentials$Params, context?: HttpContext): Observable<TokenResponse> {
    const resp = this.clientCredentials$Response(params, context);
    return resp.pipe(
      map((r: StrictHttpResponse<TokenResponse>): TokenResponse => r.body)
    );
  }

  clientCredentials$XWwwFormUrlencoded$Response(params: ClientCredentials$XWwwFormUrlencoded$Params, context?: HttpContext): Observable<StrictHttpResponse<TokenResponse>> {
    const obs = clientCredentials$XWwwFormUrlencoded(this.http, this.rootUrl, params, context);
    return obs;
  }

  clientCredentials$XWwwFormUrlencoded(params: ClientCredentials$XWwwFormUrlencoded$Params, context?: HttpContext): Observable<TokenResponse> {
    const resp = this.clientCredentials$XWwwFormUrlencoded$Response(params, context);
    return resp.pipe(
      map((r: StrictHttpResponse<TokenResponse>): TokenResponse => r.body)
    );
  }

}
