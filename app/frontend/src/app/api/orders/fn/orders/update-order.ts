import { HttpClient, HttpContext, HttpResponse } from '@angular/common/http';
import { Observable } from 'rxjs';
import { filter, map } from 'rxjs/operators';
import { StrictHttpResponse } from '../../strict-http-response';
import { RequestBuilder } from '../../request-builder';

import { OrderResponse } from '../../models/order-response';
import { UpdateOrderRequest } from '../../models/update-order-request';

export interface UpdateOrder$Params {
  orderId: string;
      body: UpdateOrderRequest
}

export function updateOrder(http: HttpClient, rootUrl: string, params: UpdateOrder$Params, context?: HttpContext): Observable<StrictHttpResponse<OrderResponse>> {
  const rb = new RequestBuilder(rootUrl, updateOrder.PATH, 'put');
  if (params) {
    rb.path('orderId', params.orderId, {});
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

updateOrder.PATH = '/orders/{orderId}';
