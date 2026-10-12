import { HttpClient, HttpContext } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';

import { BaseService } from '../base-service';
import { ApiConfiguration } from '../api-configuration';
import { StrictHttpResponse } from '../strict-http-response';

import { BondResponse } from '../models/bond-response';
import { getBond } from '../fn/bonds/get-bond';
import { GetBond$Params } from '../fn/bonds/get-bond';
import { getBonds } from '../fn/bonds/get-bonds';
import { GetBonds$Params } from '../fn/bonds/get-bonds';

@Injectable({ providedIn: 'root' })
export class BondsService extends BaseService {
  constructor(config: ApiConfiguration, http: HttpClient) {
    super(config, http);
  }

  static readonly GetBondsPath = '/bonds';

  getBonds$Response(params?: GetBonds$Params, context?: HttpContext): Observable<StrictHttpResponse<Array<BondResponse>>> {
    const obs = getBonds(this.http, this.rootUrl, params, context);
    return obs;
  }

  getBonds(params?: GetBonds$Params, context?: HttpContext): Observable<Array<BondResponse>> {
    const resp = this.getBonds$Response(params, context);
    return resp.pipe(
      map((r: StrictHttpResponse<Array<BondResponse>>): Array<BondResponse> => r.body)
    );
  }

  static readonly GetBondPath = '/bonds/{symbol}';

  getBond$Response(params: GetBond$Params, context?: HttpContext): Observable<StrictHttpResponse<BondResponse>> {
    const obs = getBond(this.http, this.rootUrl, params, context);
    return obs;
  }

  getBond(params: GetBond$Params, context?: HttpContext): Observable<BondResponse> {
    const resp = this.getBond$Response(params, context);
    return resp.pipe(
      map((r: StrictHttpResponse<BondResponse>): BondResponse => r.body)
    );
  }

}
