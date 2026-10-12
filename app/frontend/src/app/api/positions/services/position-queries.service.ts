import { HttpClient, HttpContext } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';

import { BaseService } from '../base-service';
import { ApiConfiguration } from '../api-configuration';
import { StrictHttpResponse } from '../strict-http-response';

import { getPosition } from '../fn/position-queries/get-position';
import { GetPosition$Params } from '../fn/position-queries/get-position';
import { getPositionsByAccount } from '../fn/position-queries/get-positions-by-account';
import { GetPositionsByAccount$Params } from '../fn/position-queries/get-positions-by-account';
import { Position } from '../models/position';

@Injectable({ providedIn: 'root' })
export class PositionQueriesService extends BaseService {
  constructor(config: ApiConfiguration, http: HttpClient) {
    super(config, http);
  }

  static readonly GetPositionsByAccountPath = '/positions/{accountId}';

  getPositionsByAccount$Response(params: GetPositionsByAccount$Params, context?: HttpContext): Observable<StrictHttpResponse<Array<Position>>> {
    const obs = getPositionsByAccount(this.http, this.rootUrl, params, context);
    return obs;
  }

  getPositionsByAccount(params: GetPositionsByAccount$Params, context?: HttpContext): Observable<Array<Position>> {
    const resp = this.getPositionsByAccount$Response(params, context);
    return resp.pipe(
      map((r: StrictHttpResponse<Array<Position>>): Array<Position> => r.body)
    );
  }

  static readonly GetPositionPath = '/positions/{accountId}/{symbol}';

  getPosition$Response(params: GetPosition$Params, context?: HttpContext): Observable<StrictHttpResponse<Position>> {
    const obs = getPosition(this.http, this.rootUrl, params, context);
    return obs;
  }

  getPosition(params: GetPosition$Params, context?: HttpContext): Observable<Position> {
    const resp = this.getPosition$Response(params, context);
    return resp.pipe(
      map((r: StrictHttpResponse<Position>): Position => r.body)
    );
  }

}
