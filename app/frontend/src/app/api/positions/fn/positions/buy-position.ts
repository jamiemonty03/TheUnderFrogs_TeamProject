import { HttpClient, HttpContext, HttpResponse } from '@angular/common/http';
import { Observable } from 'rxjs';
import { filter, map } from 'rxjs/operators';
import { StrictHttpResponse } from '../../strict-http-response';
import { RequestBuilder } from '../../request-builder';

import { BuyRequest } from '../../models/buy-request';
import { Position } from '../../models/position';

export interface BuyPosition$Params {

  accountId: string;

  symbol: string;
      body: BuyRequest
}

export function buyPosition(http: HttpClient, rootUrl: string, params: BuyPosition$Params, context?: HttpContext): Observable<StrictHttpResponse<Position>> {
  const rb = new RequestBuilder(rootUrl, buyPosition.PATH, 'post');
  if (params) {
    rb.path('accountId', params.accountId, {});
    rb.path('symbol', params.symbol, {});
    rb.body(params.body, 'application/json');
  }

  return http.request(
    rb.build({ responseType: 'json', accept: 'application/json', context })
  ).pipe(
    filter((r: any): r is HttpResponse<any> => r instanceof HttpResponse),
    map((r: HttpResponse<any>) => {
      return r as StrictHttpResponse<Position>;
    })
  );
}

buyPosition.PATH = '/positions/{accountId}/{symbol}/buy';
