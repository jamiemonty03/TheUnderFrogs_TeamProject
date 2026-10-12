import { HttpClient, HttpContext, HttpResponse } from '@angular/common/http';
import { Observable } from 'rxjs';
import { filter, map } from 'rxjs/operators';
import { StrictHttpResponse } from '../../strict-http-response';
import { RequestBuilder } from '../../request-builder';

import { EtfResponse } from '../../models/etf-response';

export interface GetEtf$Params {
  symbol: string;
}

export function getEtf(http: HttpClient, rootUrl: string, params: GetEtf$Params, context?: HttpContext): Observable<StrictHttpResponse<EtfResponse>> {
  const rb = new RequestBuilder(rootUrl, getEtf.PATH, 'get');
  if (params) {
    rb.path('symbol', params.symbol, {});
  }

  return http.request(
    rb.build({ responseType: 'json', accept: 'application/json', context })
  ).pipe(
    filter((r: any): r is HttpResponse<any> => r instanceof HttpResponse),
    map((r: HttpResponse<any>) => {
      return r as StrictHttpResponse<EtfResponse>;
    })
  );
}

getEtf.PATH = '/etfs/{symbol}';
