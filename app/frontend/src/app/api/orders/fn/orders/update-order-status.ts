import { HttpClient, HttpContext, HttpResponse } from '@angular/common/http';
import { Observable } from 'rxjs';
import { filter, map } from 'rxjs/operators';
import { StrictHttpResponse } from '../../strict-http-response';
import { RequestBuilder } from '../../request-builder';

import { OrderResponse } from '../../models/order-response';
import { UpdateOrderStatusRequest } from '../../models/update-order-status-request';

export interface UpdateOrderStatus$Params {
  orderId: string;
      body: UpdateOrderStatusRequest
}

export function updateOrderStatus(http: HttpClient, rootUrl: string, params: UpdateOrderStatus$Params, context?: HttpContext): Observable<StrictHttpResponse<OrderResponse>> {
  const rb = new RequestBuilder(rootUrl, updateOrderStatus.PATH, 'patch');
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

updateOrderStatus.PATH = '/orders/{orderId}/status';
