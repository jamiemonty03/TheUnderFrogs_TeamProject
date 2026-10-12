import { HttpClient, HttpContext, HttpResponse } from '@angular/common/http';
import { Observable } from 'rxjs';
import { filter, map } from 'rxjs/operators';
import { StrictHttpResponse } from '../../strict-http-response';
import { RequestBuilder } from '../../request-builder';

import { ClientTradeResponse } from '../../models/client-trade-response';

export interface GetClientTradesByAccount$Params {

  accountId: string;
}

export function getClientTradesByAccount(http: HttpClient, rootUrl: string, params: GetClientTradesByAccount$Params, context?: HttpContext): Observable<StrictHttpResponse<Array<ClientTradeResponse>>> {
  const rb = new RequestBuilder(rootUrl, getClientTradesByAccount.PATH, 'get');
  if (params) {
    rb.path('accountId', params.accountId, {});
  }

  return http.request(
    rb.build({ responseType: 'json', accept: 'application/json', context })
  ).pipe(
    filter((r: any): r is HttpResponse<any> => r instanceof HttpResponse),
    map((r: HttpResponse<any>) => {
      return r as StrictHttpResponse<Array<ClientTradeResponse>>;
    })
  );
}

getClientTradesByAccount.PATH = '/client-trades/account/{accountId}';
