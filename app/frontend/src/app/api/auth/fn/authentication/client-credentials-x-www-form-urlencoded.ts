import { HttpClient, HttpContext, HttpResponse } from '@angular/common/http';
import { Observable } from 'rxjs';
import { filter, map } from 'rxjs/operators';
import { StrictHttpResponse } from '../../strict-http-response';
import { RequestBuilder } from '../../request-builder';

import { ClientCredentialsRequest } from '../../models/client-credentials-request';
import { TokenResponse } from '../../models/token-response';

export interface ClientCredentials$XWwwFormUrlencoded$Params {
      body: ClientCredentialsRequest
}

export function clientCredentials$XWwwFormUrlencoded(http: HttpClient, rootUrl: string, params: ClientCredentials$XWwwFormUrlencoded$Params, context?: HttpContext): Observable<StrictHttpResponse<TokenResponse>> {
  const rb = new RequestBuilder(rootUrl, clientCredentials$XWwwFormUrlencoded.PATH, 'post');
  if (params) {
    rb.body(params.body, 'application/x-www-form-urlencoded');
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

clientCredentials$XWwwFormUrlencoded.PATH = '/auth/token';
