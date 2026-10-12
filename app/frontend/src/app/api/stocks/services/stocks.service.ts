import { HttpClient, HttpContext } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';

import { BaseService } from '../base-service';
import { ApiConfiguration } from '../api-configuration';
import { StrictHttpResponse } from '../strict-http-response';

import { getStock } from '../fn/stocks/get-stock';
import { GetStock$Params } from '../fn/stocks/get-stock';
import { getStocks } from '../fn/stocks/get-stocks';
import { GetStocks$Params } from '../fn/stocks/get-stocks';
import { StockResponse } from '../models/stock-response';

@Injectable({ providedIn: 'root' })
export class StocksService extends BaseService {
  constructor(config: ApiConfiguration, http: HttpClient) {
    super(config, http);
  }

  static readonly GetStocksPath = '/stocks';

  getStocks$Response(params?: GetStocks$Params, context?: HttpContext): Observable<StrictHttpResponse<Array<StockResponse>>> {
    const obs = getStocks(this.http, this.rootUrl, params, context);
    return obs;
  }

  getStocks(params?: GetStocks$Params, context?: HttpContext): Observable<Array<StockResponse>> {
    const resp = this.getStocks$Response(params, context);
    return resp.pipe(
      map((r: StrictHttpResponse<Array<StockResponse>>): Array<StockResponse> => r.body)
    );
  }

  static readonly GetStockPath = '/stocks/{symbol}';

  getStock$Response(params: GetStock$Params, context?: HttpContext): Observable<StrictHttpResponse<StockResponse>> {
    const obs = getStock(this.http, this.rootUrl, params, context);
    return obs;
  }

  getStock(params: GetStock$Params, context?: HttpContext): Observable<StockResponse> {
    const resp = this.getStock$Response(params, context);
    return resp.pipe(
      map((r: StrictHttpResponse<StockResponse>): StockResponse => r.body)
    );
  }

}
