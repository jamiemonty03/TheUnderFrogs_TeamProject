import { HttpClient, HttpContext, HttpResponse } from '@angular/common/http';
import { Observable } from 'rxjs';
import { filter, map } from 'rxjs/operators';
import { StrictHttpResponse } from '../../strict-http-response';
import { RequestBuilder } from '../../request-builder';

import { Account } from '../../models/account';
import { CreditDebitRequest } from '../../models/credit-debit-request';

export interface CreditAccount$Params {

  accountId: string;
      body: CreditDebitRequest
}

export function creditAccount(http: HttpClient, rootUrl: string, params: CreditAccount$Params, context?: HttpContext): Observable<StrictHttpResponse<Account>> {
  const rb = new RequestBuilder(rootUrl, creditAccount.PATH, 'post');
  if (params) {
    rb.path('accountId', params.accountId, {});
    rb.body(params.body, 'application/json');
  }

  return http.request(
    rb.build({ responseType: 'json', accept: 'application/json', context })
  ).pipe(
    filter((r: any): r is HttpResponse<any> => r instanceof HttpResponse),
    map((r: HttpResponse<any>) => {
      return r as StrictHttpResponse<Account>;
    })
  );
}

creditAccount.PATH = '/accounts/{accountId}/credit';
