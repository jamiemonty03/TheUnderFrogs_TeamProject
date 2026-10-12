import { HttpClient, HttpContext, HttpResponse } from '@angular/common/http';
import { Observable } from 'rxjs';
import { filter, map } from 'rxjs/operators';
import { StrictHttpResponse } from '../../strict-http-response';
import { RequestBuilder } from '../../request-builder';

import { Position } from '../../models/position';
import { ReplacePositionRequest } from '../../models/replace-position-request';

export interface UpdatePosition$Params {

  accountId: string;

  symbol: string;
      body: ReplacePositionRequest
}

export function updatePosition(http: HttpClient, rootUrl: string, params: UpdatePosition$Params, context?: HttpContext): Observable<StrictHttpResponse<Position>> {
  const rb = new RequestBuilder(rootUrl, updatePosition.PATH, 'put');
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

updatePosition.PATH = '/positions/{accountId}/{symbol}';
