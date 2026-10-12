import { HttpClient, HttpContext } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';

import { BaseService } from '../base-service';
import { ApiConfiguration } from '../api-configuration';
import { StrictHttpResponse } from '../strict-http-response';

import { Account } from '../models/account';
import { createAccount } from '../fn/accounts/create-account';
import { CreateAccount$Params } from '../fn/accounts/create-account';
import { deleteAccount } from '../fn/accounts/delete-account';
import { DeleteAccount$Params } from '../fn/accounts/delete-account';
import { getAccount } from '../fn/accounts/get-account';
import { GetAccount$Params } from '../fn/accounts/get-account';
import { listAccounts } from '../fn/accounts/list-accounts';
import { ListAccounts$Params } from '../fn/accounts/list-accounts';
import { updateAccount } from '../fn/accounts/update-account';
import { UpdateAccount$Params } from '../fn/accounts/update-account';

@Injectable({ providedIn: 'root' })
export class AccountsService extends BaseService {
  constructor(config: ApiConfiguration, http: HttpClient) {
    super(config, http);
  }

  static readonly ListAccountsPath = '/accounts';

  listAccounts$Response(params?: ListAccounts$Params, context?: HttpContext): Observable<StrictHttpResponse<Array<Account>>> {
    const obs = listAccounts(this.http, this.rootUrl, params, context);
    return obs;
  }

  listAccounts(params?: ListAccounts$Params, context?: HttpContext): Observable<Array<Account>> {
    const resp = this.listAccounts$Response(params, context);
    return resp.pipe(
      map((r: StrictHttpResponse<Array<Account>>): Array<Account> => r.body)
    );
  }

  static readonly CreateAccountPath = '/accounts';

  createAccount$Response(params: CreateAccount$Params, context?: HttpContext): Observable<StrictHttpResponse<Account>> {
    const obs = createAccount(this.http, this.rootUrl, params, context);
    return obs;
  }

  createAccount(params: CreateAccount$Params, context?: HttpContext): Observable<Account> {
    const resp = this.createAccount$Response(params, context);
    return resp.pipe(
      map((r: StrictHttpResponse<Account>): Account => r.body)
    );
  }

  static readonly GetAccountPath = '/accounts/{accountId}';

  getAccount$Response(params: GetAccount$Params, context?: HttpContext): Observable<StrictHttpResponse<Account>> {
    const obs = getAccount(this.http, this.rootUrl, params, context);
    return obs;
  }

  getAccount(params: GetAccount$Params, context?: HttpContext): Observable<Account> {
    const resp = this.getAccount$Response(params, context);
    return resp.pipe(
      map((r: StrictHttpResponse<Account>): Account => r.body)
    );
  }

  static readonly UpdateAccountPath = '/accounts/{accountId}';

  updateAccount$Response(params: UpdateAccount$Params, context?: HttpContext): Observable<StrictHttpResponse<Account>> {
    const obs = updateAccount(this.http, this.rootUrl, params, context);
    return obs;
  }

  updateAccount(params: UpdateAccount$Params, context?: HttpContext): Observable<Account> {
    const resp = this.updateAccount$Response(params, context);
    return resp.pipe(
      map((r: StrictHttpResponse<Account>): Account => r.body)
    );
  }

  static readonly DeleteAccountPath = '/accounts/{accountId}';

  deleteAccount$Response(params: DeleteAccount$Params, context?: HttpContext): Observable<StrictHttpResponse<void>> {
    const obs = deleteAccount(this.http, this.rootUrl, params, context);
    return obs;
  }

  deleteAccount(params: DeleteAccount$Params, context?: HttpContext): Observable<void> {
    const resp = this.deleteAccount$Response(params, context);
    return resp.pipe(
      map((r: StrictHttpResponse<void>): void => r.body)
    );
  }

}
