import { HttpClient, HttpContext, HttpResponse } from '@angular/common/http';
import { Observable } from 'rxjs';
import { filter, map } from 'rxjs/operators';
import { StrictHttpResponse } from '../../strict-http-response';
import { RequestBuilder } from '../../request-builder';

import { CreatePositionRequest } from '../../models/create-position-request';
import { Position } from '../../models/position';

export interface CreatePosition$Params {
      body: CreatePositionRequest
}

export function createPosition(http: HttpClient, rootUrl: string, params: CreatePosition$Params, context?: HttpContext): Observable<StrictHttpResponse<Position>> {
  const rb = new RequestBuilder(rootUrl, createPosition.PATH, 'post');
  if (params) {
    rb.body(params.body, 'application/json');
  }

  return http.request(
    rb.build({ responseType: 'json', accept: 'application/json', context })
  ).pipe(
    filter((r: any): r is HttpResponse<any> => r instanceof HttpResponse),
    map((r: HttpResponse<any>) => {
      return r as StrictHttpResponse<Position>;
    })
  );
}

createPosition.PATH = '/positions';
