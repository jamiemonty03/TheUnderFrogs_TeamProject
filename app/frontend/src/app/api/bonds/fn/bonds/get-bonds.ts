import { HttpClient, HttpContext, HttpResponse } from '@angular/common/http';
import { Observable } from 'rxjs';
import { filter, map } from 'rxjs/operators';
import { StrictHttpResponse } from '../../strict-http-response';
import { RequestBuilder } from '../../request-builder';

import { BondResponse } from '../../models/bond-response';

export interface GetBonds$Params {
}

export function getBonds(http: HttpClient, rootUrl: string, params?: GetBonds$Params, context?: HttpContext): Observable<StrictHttpResponse<Array<BondResponse>>> {
  const rb = new RequestBuilder(rootUrl, getBonds.PATH, 'get');
  if (params) {
  }

  return http.request(
    rb.build({ responseType: 'json', accept: 'application/json', context })
  ).pipe(
    filter((r: any): r is HttpResponse<any> => r instanceof HttpResponse),
    map((r: HttpResponse<any>) => {
      return r as StrictHttpResponse<Array<BondResponse>>;
    })
  );
}

getBonds.PATH = '/bonds';
