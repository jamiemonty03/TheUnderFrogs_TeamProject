import { EnvironmentProviders, makeEnvironmentProviders } from '@angular/core';
import { provideApiConfiguration as provideAuthApiConfiguration } from './api/auth/api-configuration';
import { provideApiConfiguration as provideAccountsApiConfiguration } from './api/accounts/api-configuration';
import { provideApiConfiguration as provideOrdersApiConfiguration } from './api/orders/api-configuration';
import { provideApiConfiguration as providePositionsApiConfiguration } from './api/positions/api-configuration';
import { provideApiConfiguration as provideInstrumentsApiConfiguration } from './api/instruments/api-configuration';
import { provideApiConfiguration as provideStocksApiConfiguration } from './api/stocks/api-configuration';
import { provideApiConfiguration as provideEtfsApiConfiguration } from './api/etfs/api-configuration';
import { provideApiConfiguration as provideBondsApiConfiguration } from './api/bonds/api-configuration';
import { environment } from '../environments/environment';

export interface ApiBaseUrls {
  auth: string;
  accounts: string;
  orders: string;
  positions: string;
  instruments: string;
  stocks: string;
  etfs: string;
  bonds: string;
}

export function provideApiClients(
  baseUrls: ApiBaseUrls = environment.apiBaseUrls,
): EnvironmentProviders {
  return makeEnvironmentProviders([
    provideAuthApiConfiguration(baseUrls.auth),
    provideAccountsApiConfiguration(baseUrls.accounts),
    provideOrdersApiConfiguration(baseUrls.orders),
    providePositionsApiConfiguration(baseUrls.positions),
    provideInstrumentsApiConfiguration(baseUrls.instruments),
    provideStocksApiConfiguration(baseUrls.stocks),
    provideEtfsApiConfiguration(baseUrls.etfs),
    provideBondsApiConfiguration(baseUrls.bonds),
  ]);
}
