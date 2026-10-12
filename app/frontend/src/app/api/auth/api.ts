import { Injectable } from '@angular/core';
import { HttpClient, HttpContext, HttpResponse } from '@angular/common/http';
import { Observable } from 'rxjs';
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

  invoke<P, R>(fn: ApiFnRequired<P, R>, params: P, context?: HttpContext): Observable<R>;
  invoke<P, R>(fn: ApiFnOptional<P, R>, params?: P, context?: HttpContext): Observable<R>;
  invoke<P, R>(fn: ApiFnRequired<P, R> | ApiFnOptional<P, R>, params: P, context?: HttpContext): Observable<R> {
    const resp = this.invoke$Response(fn, params, context);
    return resp.pipe(map(r => r.body));
  }

  invoke$Response<P, R>(fn: ApiFnRequired<P, R>, params: P, context?: HttpContext): Observable<StrictHttpResponse<R>>;
  invoke$Response<P, R>(fn: ApiFnOptional<P, R>, params?: P, context?: HttpContext): Observable<StrictHttpResponse<R>>;
  invoke$Response<P, R>(fn: ApiFnRequired<P, R> | ApiFnOptional<P, R>, params: P, context?: HttpContext): Observable<StrictHttpResponse<R>> {
    const obs = fn(this.http, this.rootUrl, params, context)
      .pipe(
        filter(r => r instanceof HttpResponse),
        map(r => r as StrictHttpResponse<R>));
    return obs;
  }
}
