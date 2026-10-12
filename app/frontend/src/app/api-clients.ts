import { EnvironmentProviders, makeEnvironmentProviders } from '@angular/core';
import { provideApiConfiguration as provideAuthApiConfiguration } from './api/auth/api-configuration';
import { environment } from '../environments/environment';

export interface ApiBaseUrls {
  auth: string;
}

export function provideApiClients(
  baseUrls: ApiBaseUrls = environment.apiBaseUrls,
): EnvironmentProviders {
  return makeEnvironmentProviders([provideAuthApiConfiguration(baseUrls.auth)]);
}
