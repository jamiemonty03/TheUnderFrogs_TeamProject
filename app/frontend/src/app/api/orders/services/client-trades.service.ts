import { HttpClient, HttpContext } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';

import { BaseService } from '../base-service';
import { ApiConfiguration } from '../api-configuration';
import { StrictHttpResponse } from '../strict-http-response';

import { ClientTradeResponse } from '../models/client-trade-response';
import { getClientTradeById } from '../fn/client-trades/get-client-trade-by-id';
import { GetClientTradeById$Params } from '../fn/client-trades/get-client-trade-by-id';
import { getClientTradesByAccount } from '../fn/client-trades/get-client-trades-by-account';
import { GetClientTradesByAccount$Params } from '../fn/client-trades/get-client-trades-by-account';
import { searchClientTrades } from '../fn/client-trades/search-client-trades';
import { SearchClientTrades$Params } from '../fn/client-trades/search-client-trades';

@Injectable({ providedIn: 'root' })
export class ClientTradesService extends BaseService {
  constructor(config: ApiConfiguration, http: HttpClient) {
    super(config, http);
  }

  static readonly SearchClientTradesPath = '/client-trades';

  searchClientTrades$Response(params?: SearchClientTrades$Params, context?: HttpContext): Observable<StrictHttpResponse<Array<ClientTradeResponse>>> {
    const obs = searchClientTrades(this.http, this.rootUrl, params, context);
    return obs;
  }

  searchClientTrades(params?: SearchClientTrades$Params, context?: HttpContext): Observable<Array<ClientTradeResponse>> {
    const resp = this.searchClientTrades$Response(params, context);
    return resp.pipe(
      map((r: StrictHttpResponse<Array<ClientTradeResponse>>): Array<ClientTradeResponse> => r.body)
    );
  }

  static readonly GetClientTradeByIdPath = '/client-trades/{tradeId}';

  getClientTradeById$Response(params: GetClientTradeById$Params, context?: HttpContext): Observable<StrictHttpResponse<ClientTradeResponse>> {
    const obs = getClientTradeById(this.http, this.rootUrl, params, context);
    return obs;
  }

  getClientTradeById(params: GetClientTradeById$Params, context?: HttpContext): Observable<ClientTradeResponse> {
    const resp = this.getClientTradeById$Response(params, context);
    return resp.pipe(
      map((r: StrictHttpResponse<ClientTradeResponse>): ClientTradeResponse => r.body)
    );
  }

  static readonly GetClientTradesByAccountPath = '/client-trades/account/{accountId}';

  getClientTradesByAccount$Response(params: GetClientTradesByAccount$Params, context?: HttpContext): Observable<StrictHttpResponse<Array<ClientTradeResponse>>> {
    const obs = getClientTradesByAccount(this.http, this.rootUrl, params, context);
    return obs;
  }

  getClientTradesByAccount(params: GetClientTradesByAccount$Params, context?: HttpContext): Observable<Array<ClientTradeResponse>> {
    const resp = this.getClientTradesByAccount$Response(params, context);
    return resp.pipe(
      map((r: StrictHttpResponse<Array<ClientTradeResponse>>): Array<ClientTradeResponse> => r.body)
    );
  }

}
