import { HttpClient, HttpContext } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';

import { BaseService } from '../base-service';
import { ApiConfiguration } from '../api-configuration';
import { StrictHttpResponse } from '../strict-http-response';

import { EtfResponse } from '../models/etf-response';
import { getEtf } from '../fn/et-fs/get-etf';
import { GetEtf$Params } from '../fn/et-fs/get-etf';
import { getEtfs } from '../fn/et-fs/get-etfs';
import { GetEtfs$Params } from '../fn/et-fs/get-etfs';

@Injectable({ providedIn: 'root' })
export class EtFsService extends BaseService {
  constructor(config: ApiConfiguration, http: HttpClient) {
    super(config, http);
  }

  static readonly GetEtfsPath = '/etfs';

  getEtfs$Response(params?: GetEtfs$Params, context?: HttpContext): Observable<StrictHttpResponse<Array<EtfResponse>>> {
    const obs = getEtfs(this.http, this.rootUrl, params, context);
    return obs;
  }

  getEtfs(params?: GetEtfs$Params, context?: HttpContext): Observable<Array<EtfResponse>> {
    const resp = this.getEtfs$Response(params, context);
    return resp.pipe(
      map((r: StrictHttpResponse<Array<EtfResponse>>): Array<EtfResponse> => r.body)
    );
  }

  static readonly GetEtfPath = '/etfs/{symbol}';

  getEtf$Response(params: GetEtf$Params, context?: HttpContext): Observable<StrictHttpResponse<EtfResponse>> {
    const obs = getEtf(this.http, this.rootUrl, params, context);
    return obs;
  }

  getEtf(params: GetEtf$Params, context?: HttpContext): Observable<EtfResponse> {
    const resp = this.getEtf$Response(params, context);
    return resp.pipe(
      map((r: StrictHttpResponse<EtfResponse>): EtfResponse => r.body)
    );
  }

}
