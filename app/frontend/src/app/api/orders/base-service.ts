import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { ApiConfiguration } from './api-configuration';

@Injectable()
export class BaseService {
  constructor(
    protected config: ApiConfiguration,
    protected http: HttpClient
  ) {
  }

  private _rootUrl?: string;

  get rootUrl(): string {
    return this._rootUrl || this.config.rootUrl;
  }

  set rootUrl(rootUrl: string) {
    this._rootUrl = rootUrl;
  }
}
