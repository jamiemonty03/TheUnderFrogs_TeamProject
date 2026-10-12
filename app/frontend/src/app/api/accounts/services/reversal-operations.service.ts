import { HttpClient, HttpContext } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';

import { BaseService } from '../base-service';
import { ApiConfiguration } from '../api-configuration';
import { StrictHttpResponse } from '../strict-http-response';

import { Account } from '../models/account';
import { reverseMovement } from '../fn/reversal-operations/reverse-movement';
import { ReverseMovement$Params } from '../fn/reversal-operations/reverse-movement';

@Injectable({ providedIn: 'root' })
export class ReversalOperationsService extends BaseService {
  constructor(config: ApiConfiguration, http: HttpClient) {
    super(config, http);
  }

  static readonly ReverseMovementPath = '/accounts/{accountId}/reversal';

  reverseMovement$Response(params: ReverseMovement$Params, context?: HttpContext): Observable<StrictHttpResponse<Account>> {
    const obs = reverseMovement(this.http, this.rootUrl, params, context);
    return obs;
  }

  reverseMovement(params: ReverseMovement$Params, context?: HttpContext): Observable<Account> {
    const resp = this.reverseMovement$Response(params, context);
    return resp.pipe(
      map((r: StrictHttpResponse<Account>): Account => r.body)
    );
  }

}
