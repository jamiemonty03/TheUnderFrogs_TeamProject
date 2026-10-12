import { HttpClient, HttpContext } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { firstValueFrom } from 'rxjs';

import { BaseService } from '../base-service';
import { ApiConfiguration } from '../api-configuration';
import { StrictHttpResponse } from '../strict-http-response';

import { CurrentUser } from '../models/current-user';
import { getCurrentUser } from '../fn/user/get-current-user';
import { GetCurrentUser$Params } from '../fn/user/get-current-user';

@Injectable({ providedIn: 'root' })
export class UserService extends BaseService {
  constructor(config: ApiConfiguration, http: HttpClient) {
    super(config, http);
  }

  static readonly GetCurrentUserPath = '/auth/me';

  getCurrentUser$Response(params?: GetCurrentUser$Params, context?: HttpContext): Promise<StrictHttpResponse<CurrentUser>> {
    const obs = getCurrentUser(this.http, this.rootUrl, params, context);
    return firstValueFrom(obs);
  }

  getCurrentUser(params?: GetCurrentUser$Params, context?: HttpContext): Promise<CurrentUser> {
    const resp = this.getCurrentUser$Response(params, context);
    return resp.then((r: StrictHttpResponse<CurrentUser>): CurrentUser => r.body);
  }

}
