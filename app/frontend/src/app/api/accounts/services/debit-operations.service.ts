import { HttpClient, HttpContext } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';

import { BaseService } from '../base-service';
import { ApiConfiguration } from '../api-configuration';
import { StrictHttpResponse } from '../strict-http-response';

import { Account } from '../models/account';
import { debitAccount } from '../fn/debit-operations/debit-account';
import { DebitAccount$Params } from '../fn/debit-operations/debit-account';

@Injectable({ providedIn: 'root' })
export class DebitOperationsService extends BaseService {
  constructor(config: ApiConfiguration, http: HttpClient) {
    super(config, http);
  }

  static readonly DebitAccountPath = '/accounts/{accountId}/debit';

  debitAccount$Response(params: DebitAccount$Params, context?: HttpContext): Observable<StrictHttpResponse<Account>> {
    const obs = debitAccount(this.http, this.rootUrl, params, context);
    return obs;
  }

  debitAccount(params: DebitAccount$Params, context?: HttpContext): Observable<Account> {
    const resp = this.debitAccount$Response(params, context);
    return resp.pipe(
      map((r: StrictHttpResponse<Account>): Account => r.body)
    );
  }

}
