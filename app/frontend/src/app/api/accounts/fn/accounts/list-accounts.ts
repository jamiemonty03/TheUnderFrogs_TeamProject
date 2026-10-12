import { HttpClient, HttpContext, HttpResponse } from '@angular/common/http';
import { Observable } from 'rxjs';
import { filter, map } from 'rxjs/operators';
import { StrictHttpResponse } from '../../strict-http-response';
import { RequestBuilder } from '../../request-builder';

import { Account } from '../../models/account';

export interface ListAccounts$Params {
}

export function listAccounts(http: HttpClient, rootUrl: string, params?: ListAccounts$Params, context?: HttpContext): Observable<StrictHttpResponse<Array<Account>>> {
  const rb = new RequestBuilder(rootUrl, listAccounts.PATH, 'get');
  if (params) {
  }

  return http.request(
    rb.build({ responseType: 'json', accept: 'application/json', context })
  ).pipe(
    filter((r: any): r is HttpResponse<any> => r instanceof HttpResponse),
    map((r: HttpResponse<any>) => {
      return r as StrictHttpResponse<Array<Account>>;
    })
  );
}

listAccounts.PATH = '/accounts';
