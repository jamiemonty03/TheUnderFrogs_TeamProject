import { Injectable } from '@angular/core';

export function provideApiConfiguration(rootUrl: string) {
  var config = new ApiConfiguration();
  config.rootUrl = rootUrl;
  return {
    provide: ApiConfiguration,
    useValue: config
  };
}

@Injectable({
  providedIn: 'root',
})
export class ApiConfiguration {
  rootUrl: string = '/api';
}

