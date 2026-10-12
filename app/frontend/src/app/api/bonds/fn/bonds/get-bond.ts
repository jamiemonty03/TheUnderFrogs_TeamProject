import { HttpClient, HttpContext, HttpResponse } from '@angular/common/http';
import { Observable } from 'rxjs';
import { filter, map } from 'rxjs/operators';
import { StrictHttpResponse } from '../../strict-http-response';
import { RequestBuilder } from '../../request-builder';

import { BondResponse } from '../../models/bond-response';

export interface GetBond$Params {
  symbol: string;
}

export function getBond(http: HttpClient, rootUrl: string, params: GetBond$Params, context?: HttpContext): Observable<StrictHttpResponse<BondResponse>> {
  const rb = new RequestBuilder(rootUrl, getBond.PATH, 'get');
  if (params) {
    rb.path('symbol', params.symbol, {});
  }

  return http.request(
    rb.build({ responseType: 'json', accept: 'application/json', context })
  ).pipe(
    filter((r: any): r is HttpResponse<any> => r instanceof HttpResponse),
    map((r: HttpResponse<any>) => {
      return r as StrictHttpResponse<BondResponse>;
    })
  );
}

getBond.PATH = '/bonds/{symbol}';
