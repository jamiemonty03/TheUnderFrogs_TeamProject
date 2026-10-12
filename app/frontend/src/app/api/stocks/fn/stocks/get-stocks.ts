import { HttpClient, HttpContext, HttpResponse } from '@angular/common/http';
import { Observable } from 'rxjs';
import { filter, map } from 'rxjs/operators';
import { StrictHttpResponse } from '../../strict-http-response';
import { RequestBuilder } from '../../request-builder';

import { StockResponse } from '../../models/stock-response';

export interface GetStocks$Params {
}

export function getStocks(http: HttpClient, rootUrl: string, params?: GetStocks$Params, context?: HttpContext): Observable<StrictHttpResponse<Array<StockResponse>>> {
  const rb = new RequestBuilder(rootUrl, getStocks.PATH, 'get');
  if (params) {
  }

  return http.request(
    rb.build({ responseType: 'json', accept: 'application/json', context })
  ).pipe(
    filter((r: any): r is HttpResponse<any> => r instanceof HttpResponse),
    map((r: HttpResponse<any>) => {
      return r as StrictHttpResponse<Array<StockResponse>>;
    })
  );
}

getStocks.PATH = '/stocks';
