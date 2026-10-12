import { HttpClient, HttpContext, HttpResponse } from '@angular/common/http';
import { Observable } from 'rxjs';
import { filter, map } from 'rxjs/operators';
import { StrictHttpResponse } from '../../strict-http-response';
import { RequestBuilder } from '../../request-builder';

import { Position } from '../../models/position';
import { ReversalRequest } from '../../models/reversal-request';

export interface ReversePosition$Params {

  accountId: string;

  symbol: string;
      body: ReversalRequest
}

export function reversePosition(http: HttpClient, rootUrl: string, params: ReversePosition$Params, context?: HttpContext): Observable<StrictHttpResponse<Position>> {
  const rb = new RequestBuilder(rootUrl, reversePosition.PATH, 'post');
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

reversePosition.PATH = '/positions/{accountId}/{symbol}/reversal';
