import { HttpClient, HttpContext, HttpResponse } from '@angular/common/http';
import { Observable } from 'rxjs';
import { filter, map } from 'rxjs/operators';
import { StrictHttpResponse } from '../../strict-http-response';
import { RequestBuilder } from '../../request-builder';

import { ClientTradeResponse } from '../../models/client-trade-response';

export interface SearchClientTrades$Params {

  accountId?: string;

  from?: string;

  to?: string;

  assetClass?: 'Equity' | 'Bond' | 'ETF';

  symbol?: string;

  side?: 'BUY' | 'SELL';
}

export function searchClientTrades(http: HttpClient, rootUrl: string, params?: SearchClientTrades$Params, context?: HttpContext): Observable<StrictHttpResponse<Array<ClientTradeResponse>>> {
  const rb = new RequestBuilder(rootUrl, searchClientTrades.PATH, 'get');
  if (params) {
    rb.query('accountId', params.accountId, {});
    rb.query('from', params.from, {});
    rb.query('to', params.to, {});
    rb.query('assetClass', params.assetClass, {});
    rb.query('symbol', params.symbol, {});
    rb.query('side', params.side, {});
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

searchClientTrades.PATH = '/client-trades';
