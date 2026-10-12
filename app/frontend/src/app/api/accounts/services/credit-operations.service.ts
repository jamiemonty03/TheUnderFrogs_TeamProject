import { HttpClient, HttpContext } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';

import { BaseService } from '../base-service';
import { ApiConfiguration } from '../api-configuration';
import { StrictHttpResponse } from '../strict-http-response';

import { Account } from '../models/account';
import { creditAccount } from '../fn/credit-operations/credit-account';
import { CreditAccount$Params } from '../fn/credit-operations/credit-account';

@Injectable({ providedIn: 'root' })
export class CreditOperationsService extends BaseService {
  constructor(config: ApiConfiguration, http: HttpClient) {
    super(config, http);
  }

  static readonly CreditAccountPath = '/accounts/{accountId}/credit';

  creditAccount$Response(params: CreditAccount$Params, context?: HttpContext): Observable<StrictHttpResponse<Account>> {
    const obs = creditAccount(this.http, this.rootUrl, params, context);
    return obs;
  }

  creditAccount(params: CreditAccount$Params, context?: HttpContext): Observable<Account> {
    const resp = this.creditAccount$Response(params, context);
    return resp.pipe(
      map((r: StrictHttpResponse<Account>): Account => r.body)
    );
  }

}
