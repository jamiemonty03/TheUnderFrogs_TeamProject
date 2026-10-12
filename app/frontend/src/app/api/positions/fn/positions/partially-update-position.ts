import { HttpClient, HttpContext, HttpResponse } from '@angular/common/http';
import { Observable } from 'rxjs';
import { filter, map } from 'rxjs/operators';
import { StrictHttpResponse } from '../../strict-http-response';
import { RequestBuilder } from '../../request-builder';

import { Position } from '../../models/position';
import { UpdatePositionRequest } from '../../models/update-position-request';

export interface PartiallyUpdatePosition$Params {

  accountId: string;

  symbol: string;
      body?: UpdatePositionRequest
}

export function partiallyUpdatePosition(http: HttpClient, rootUrl: string, params: PartiallyUpdatePosition$Params, context?: HttpContext): Observable<StrictHttpResponse<Position>> {
  const rb = new RequestBuilder(rootUrl, partiallyUpdatePosition.PATH, 'patch');
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

partiallyUpdatePosition.PATH = '/positions/{accountId}/{symbol}';
