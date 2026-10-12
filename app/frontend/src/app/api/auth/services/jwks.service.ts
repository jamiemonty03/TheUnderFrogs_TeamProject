import { HttpClient, HttpContext } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';

import { BaseService } from '../base-service';
import { ApiConfiguration } from '../api-configuration';
import { StrictHttpResponse } from '../strict-http-response';

import { getJwks } from '../fn/jwks/get-jwks';
import { GetJwks$Params } from '../fn/jwks/get-jwks';
import { Jwks } from '../models/jwks';

@Injectable({ providedIn: 'root' })
export class JwksService extends BaseService {
  constructor(config: ApiConfiguration, http: HttpClient) {
    super(config, http);
  }

  static readonly GetJwksPath = '/.well-known/jwks.json';

  getJwks$Response(params?: GetJwks$Params, context?: HttpContext): Observable<StrictHttpResponse<Jwks>> {
    const obs = getJwks(this.http, this.rootUrl, params, context);
    return obs;
  }

  getJwks(params?: GetJwks$Params, context?: HttpContext): Observable<Jwks> {
    const resp = this.getJwks$Response(params, context);
    return resp.pipe(
      map((r: StrictHttpResponse<Jwks>): Jwks => r.body)
    );
  }

}
