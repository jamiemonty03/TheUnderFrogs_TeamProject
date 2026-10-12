import { HttpClient, HttpContext, HttpResponse } from '@angular/common/http';
import { Observable } from 'rxjs';
import { filter, map } from 'rxjs/operators';
import { StrictHttpResponse } from '../../strict-http-response';
import { RequestBuilder } from '../../request-builder';

import { OrderResponse } from '../../models/order-response';
import { PlaceOrderRequest } from '../../models/place-order-request';

export interface PlaceOrder$Params {
      body: PlaceOrderRequest
}

export function placeOrder(http: HttpClient, rootUrl: string, params: PlaceOrder$Params, context?: HttpContext): Observable<StrictHttpResponse<OrderResponse>> {
  const rb = new RequestBuilder(rootUrl, placeOrder.PATH, 'post');
  if (params) {
    rb.body(params.body, 'application/json');
  }

  return http.request(
    rb.build({ responseType: 'json', accept: 'application/json', context })
  ).pipe(
    filter((r: any): r is HttpResponse<any> => r instanceof HttpResponse),
    map((r: HttpResponse<any>) => {
      return r as StrictHttpResponse<OrderResponse>;
    })
  );
}

placeOrder.PATH = '/orders';
