import { HttpClient, HttpContext } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { firstValueFrom } from 'rxjs';

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

  getJwks$Response(params?: GetJwks$Params, context?: HttpContext): Promise<StrictHttpResponse<Jwks>> {
    const obs = getJwks(this.http, this.rootUrl, params, context);
    return firstValueFrom(obs);
  }

  getJwks(params?: GetJwks$Params, context?: HttpContext): Promise<Jwks> {
    const resp = this.getJwks$Response(params, context);
    return resp.then((r: StrictHttpResponse<Jwks>): Jwks => r.body);
  }

}
