import { HttpClient, HttpContext, HttpResponse } from '@angular/common/http';
import { Observable } from 'rxjs';
import { filter, map } from 'rxjs/operators';
import { StrictHttpResponse } from '../../strict-http-response';
import { RequestBuilder } from '../../request-builder';

import { InstrumentResponse } from '../../models/instrument-response';

export interface GetInstruments$Params {
}

export function getInstruments(http: HttpClient, rootUrl: string, params?: GetInstruments$Params, context?: HttpContext): Observable<StrictHttpResponse<Array<InstrumentResponse>>> {
  const rb = new RequestBuilder(rootUrl, getInstruments.PATH, 'get');
  if (params) {
  }

  return http.request(
    rb.build({ responseType: 'json', accept: 'application/json', context })
  ).pipe(
    filter((r: any): r is HttpResponse<any> => r instanceof HttpResponse),
    map((r: HttpResponse<any>) => {
      return r as StrictHttpResponse<Array<InstrumentResponse>>;
    })
  );
}

getInstruments.PATH = '/instruments';
