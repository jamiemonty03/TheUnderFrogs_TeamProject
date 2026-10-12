export { ApiConfiguration } from './api-configuration';
export { provideApiConfiguration } from './api-configuration';
export { RequestBuilder } from './request-builder';
export type { StrictHttpResponse } from './strict-http-response';
export { Api } from './api';

export type { ErrorResponse } from './models/error-response';
export type { StockResponse } from './models/stock-response';
export { BaseService } from './base-service';
export { StocksService } from './services/stocks.service';

export type { GetStocks$Params as GetStocks$Params } from './fn/stocks/get-stocks';
export { getStocks as getStocks } from './fn/stocks/get-stocks';
export type { GetStock$Params as GetStock$Params } from './fn/stocks/get-stock';
export { getStock as getStock } from './fn/stocks/get-stock';
