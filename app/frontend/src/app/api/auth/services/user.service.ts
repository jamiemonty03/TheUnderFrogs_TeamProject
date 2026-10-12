import { HttpClient, HttpContext } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';

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

  getCurrentUser$Response(params?: GetCurrentUser$Params, context?: HttpContext): Observable<StrictHttpResponse<CurrentUser>> {
    const obs = getCurrentUser(this.http, this.rootUrl, params, context);
    return obs;
  }

  getCurrentUser(params?: GetCurrentUser$Params, context?: HttpContext): Observable<CurrentUser> {
    const resp = this.getCurrentUser$Response(params, context);
    return resp.pipe(
      map((r: StrictHttpResponse<CurrentUser>): CurrentUser => r.body)
    );
  }

}
