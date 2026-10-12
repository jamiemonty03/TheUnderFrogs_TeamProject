import { HttpClient, HttpContext, HttpResponse } from '@angular/common/http';
import { Observable } from 'rxjs';
import { filter, map } from 'rxjs/operators';
import { StrictHttpResponse } from '../../strict-http-response';
import { RequestBuilder } from '../../request-builder';

export interface DeletePosition$Params {

  accountId: string;

  symbol: string;
}

export function deletePosition(http: HttpClient, rootUrl: string, params: DeletePosition$Params, context?: HttpContext): Observable<StrictHttpResponse<void>> {
  const rb = new RequestBuilder(rootUrl, deletePosition.PATH, 'delete');
  if (params) {
    rb.path('accountId', params.accountId, {});
    rb.path('symbol', params.symbol, {});
  }

  return http.request(
    rb.build({ responseType: 'text', accept: '*/*', context })
  ).pipe(
    filter((r: any): r is HttpResponse<any> => r instanceof HttpResponse),
    map((r: HttpResponse<any>) => {
      return (r as HttpResponse<any>).clone({ body: undefined }) as StrictHttpResponse<void>;
    })
  );
}

deletePosition.PATH = '/positions/{accountId}/{symbol}';
