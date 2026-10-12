import { HttpClient, HttpContext, HttpResponse } from '@angular/common/http';
import { Observable } from 'rxjs';
import { filter, map } from 'rxjs/operators';
import { StrictHttpResponse } from '../../strict-http-response';
import { RequestBuilder } from '../../request-builder';

import { StockResponse } from '../../models/stock-response';

export interface GetStock$Params {
  symbol: string;
}

export function getStock(http: HttpClient, rootUrl: string, params: GetStock$Params, context?: HttpContext): Observable<StrictHttpResponse<StockResponse>> {
  const rb = new RequestBuilder(rootUrl, getStock.PATH, 'get');
  if (params) {
    rb.path('symbol', params.symbol, {});
  }

  return http.request(
    rb.build({ responseType: 'json', accept: 'application/json', context })
  ).pipe(
    filter((r: any): r is HttpResponse<any> => r instanceof HttpResponse),
    map((r: HttpResponse<any>) => {
      return r as StrictHttpResponse<StockResponse>;
    })
  );
}

getStock.PATH = '/stocks/{symbol}';
