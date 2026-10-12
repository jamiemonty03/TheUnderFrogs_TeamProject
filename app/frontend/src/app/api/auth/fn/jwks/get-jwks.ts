import { HttpClient, HttpContext, HttpResponse } from '@angular/common/http';
import { Observable } from 'rxjs';
import { filter, map } from 'rxjs/operators';
import { StrictHttpResponse } from '../../strict-http-response';
import { RequestBuilder } from '../../request-builder';

import { Jwks } from '../../models/jwks';

export interface GetJwks$Params {
}

export function getJwks(http: HttpClient, rootUrl: string, params?: GetJwks$Params, context?: HttpContext): Observable<StrictHttpResponse<Jwks>> {
  const rb = new RequestBuilder(rootUrl, getJwks.PATH, 'get');
  if (params) {
  }

  return http.request(
    rb.build({ responseType: 'json', accept: 'application/json', context })
  ).pipe(
    filter((r: any): r is HttpResponse<any> => r instanceof HttpResponse),
    map((r: HttpResponse<any>) => {
      return r as StrictHttpResponse<Jwks>;
    })
  );
}

getJwks.PATH = '/.well-known/jwks.json';
