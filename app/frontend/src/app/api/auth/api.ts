import { Injectable } from '@angular/core';
import { HttpClient, HttpContext, HttpResponse } from '@angular/common/http';
import { Observable, firstValueFrom } from 'rxjs';
import { filter, map } from 'rxjs/operators';
import { ApiConfiguration } from './api-configuration';
import { StrictHttpResponse } from './strict-http-response';

export type ApiFnOptional<P, R> = (http: HttpClient, rootUrl: string, params?: P, context?: HttpContext) => Observable<StrictHttpResponse<R>>;
export type ApiFnRequired<P, R> = (http: HttpClient, rootUrl: string, params: P, context?: HttpContext) => Observable<StrictHttpResponse<R>>;

@Injectable({ providedIn: 'root' })
export class Api {
  constructor(
    private config: ApiConfiguration,
    private http: HttpClient
  ) {
  }

  private _rootUrl?: string;

  get rootUrl(): string {
    return this._rootUrl || this.config.rootUrl;
  }

  set rootUrl(rootUrl: string) {
    this._rootUrl = rootUrl;
  }

  invoke<P, R>(fn: ApiFnRequired<P, R>, params: P, context?: HttpContext): Promise<R>;
  invoke<P, R>(fn: ApiFnOptional<P, R>, params?: P, context?: HttpContext): Promise<R>;
  async invoke<P, R>(fn: ApiFnRequired<P, R> | ApiFnOptional<P, R>, params: P, context?: HttpContext): Promise<R> {
    const resp = this.invoke$Response(fn, params, context);
    return (await resp).body;
  }

  invoke$Response<P, R>(fn: ApiFnRequired<P, R>, params: P, context?: HttpContext): Promise<StrictHttpResponse<R>>;
  invoke$Response<P, R>(fn: ApiFnOptional<P, R>, params?: P, context?: HttpContext): Promise<StrictHttpResponse<R>>;
  invoke$Response<P, R>(fn: ApiFnRequired<P, R> | ApiFnOptional<P, R>, params: P, context?: HttpContext): Promise<StrictHttpResponse<R>> {
    const obs = fn(this.http, this.rootUrl, params, context)
      .pipe(
        filter(r => r instanceof HttpResponse),
        map(r => r as StrictHttpResponse<R>));
    return firstValueFrom(obs);
  }
}
