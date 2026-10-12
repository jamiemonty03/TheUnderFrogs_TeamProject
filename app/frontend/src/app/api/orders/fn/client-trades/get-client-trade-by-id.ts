import { HttpClient, HttpContext, HttpResponse } from '@angular/common/http';
import { Observable } from 'rxjs';
import { filter, map } from 'rxjs/operators';
import { StrictHttpResponse } from '../../strict-http-response';
import { RequestBuilder } from '../../request-builder';

import { ClientTradeResponse } from '../../models/client-trade-response';

export interface GetClientTradeById$Params {

  tradeId: number;
}

export function getClientTradeById(http: HttpClient, rootUrl: string, params: GetClientTradeById$Params, context?: HttpContext): Observable<StrictHttpResponse<ClientTradeResponse>> {
  const rb = new RequestBuilder(rootUrl, getClientTradeById.PATH, 'get');
  if (params) {
    rb.path('tradeId', params.tradeId, {});
  }

  return http.request(
    rb.build({ responseType: 'json', accept: 'application/json', context })
  ).pipe(
    filter((r: any): r is HttpResponse<any> => r instanceof HttpResponse),
    map((r: HttpResponse<any>) => {
      return r as StrictHttpResponse<ClientTradeResponse>;
    })
  );
}

getClientTradeById.PATH = '/client-trades/{tradeId}';
