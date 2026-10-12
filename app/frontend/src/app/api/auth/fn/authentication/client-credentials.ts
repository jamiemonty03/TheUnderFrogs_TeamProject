import { HttpClient, HttpContext, HttpResponse } from '@angular/common/http';
import { Observable } from 'rxjs';
import { filter, map } from 'rxjs/operators';
import { StrictHttpResponse } from '../../strict-http-response';
import { RequestBuilder } from '../../request-builder';

import { ClientCredentialsRequest } from '../../models/client-credentials-request';
import { TokenResponse } from '../../models/token-response';

export interface ClientCredentials$Params {
      body: ClientCredentialsRequest
}

export function clientCredentials(http: HttpClient, rootUrl: string, params: ClientCredentials$Params, context?: HttpContext): Observable<StrictHttpResponse<TokenResponse>> {
  const rb = new RequestBuilder(rootUrl, clientCredentials.PATH, 'post');
  if (params) {
    rb.body(params.body, 'application/json');
  }

  return http.request(
    rb.build({ responseType: 'json', accept: 'application/json', context })
  ).pipe(
    filter((r: any): r is HttpResponse<any> => r instanceof HttpResponse),
    map((r: HttpResponse<any>) => {
      return r as StrictHttpResponse<TokenResponse>;
    })
  );
}

clientCredentials.PATH = '/auth/token';
