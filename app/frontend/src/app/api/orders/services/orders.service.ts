import { HttpClient, HttpContext } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';

import { BaseService } from '../base-service';
import { ApiConfiguration } from '../api-configuration';
import { StrictHttpResponse } from '../strict-http-response';

import { deleteOrder } from '../fn/orders/delete-order';
import { DeleteOrder$Params } from '../fn/orders/delete-order';
import { getOrderById } from '../fn/orders/get-order-by-id';
import { GetOrderById$Params } from '../fn/orders/get-order-by-id';
import { getOrdersByAccount } from '../fn/orders/get-orders-by-account';
import { GetOrdersByAccount$Params } from '../fn/orders/get-orders-by-account';
import { listOrders } from '../fn/orders/list-orders';
import { ListOrders$Params } from '../fn/orders/list-orders';
import { OrderResponse } from '../models/order-response';
import { placeOrder } from '../fn/orders/place-order';
import { PlaceOrder$Params } from '../fn/orders/place-order';
import { updateOrder } from '../fn/orders/update-order';
import { UpdateOrder$Params } from '../fn/orders/update-order';
import { updateOrderStatus } from '../fn/orders/update-order-status';
import { UpdateOrderStatus$Params } from '../fn/orders/update-order-status';

@Injectable({ providedIn: 'root' })
export class OrdersService extends BaseService {
  constructor(config: ApiConfiguration, http: HttpClient) {
    super(config, http);
  }

  static readonly GetOrderByIdPath = '/orders/{orderId}';

  getOrderById$Response(params: GetOrderById$Params, context?: HttpContext): Observable<StrictHttpResponse<OrderResponse>> {
    const obs = getOrderById(this.http, this.rootUrl, params, context);
    return obs;
  }

  getOrderById(params: GetOrderById$Params, context?: HttpContext): Observable<OrderResponse> {
    const resp = this.getOrderById$Response(params, context);
    return resp.pipe(
      map((r: StrictHttpResponse<OrderResponse>): OrderResponse => r.body)
    );
  }

  static readonly UpdateOrderPath = '/orders/{orderId}';

  updateOrder$Response(params: UpdateOrder$Params, context?: HttpContext): Observable<StrictHttpResponse<OrderResponse>> {
    const obs = updateOrder(this.http, this.rootUrl, params, context);
    return obs;
  }

  updateOrder(params: UpdateOrder$Params, context?: HttpContext): Observable<OrderResponse> {
    const resp = this.updateOrder$Response(params, context);
    return resp.pipe(
      map((r: StrictHttpResponse<OrderResponse>): OrderResponse => r.body)
    );
  }

  static readonly DeleteOrderPath = '/orders/{orderId}';

  deleteOrder$Response(params: DeleteOrder$Params, context?: HttpContext): Observable<StrictHttpResponse<void>> {
    const obs = deleteOrder(this.http, this.rootUrl, params, context);
    return obs;
  }

  deleteOrder(params: DeleteOrder$Params, context?: HttpContext): Observable<void> {
    const resp = this.deleteOrder$Response(params, context);
    return resp.pipe(
      map((r: StrictHttpResponse<void>): void => r.body)
    );
  }

  static readonly UpdateOrderStatusPath = '/orders/{orderId}/status';

  updateOrderStatus$Response(params: UpdateOrderStatus$Params, context?: HttpContext): Observable<StrictHttpResponse<OrderResponse>> {
    const obs = updateOrderStatus(this.http, this.rootUrl, params, context);
    return obs;
  }

  updateOrderStatus(params: UpdateOrderStatus$Params, context?: HttpContext): Observable<OrderResponse> {
    const resp = this.updateOrderStatus$Response(params, context);
    return resp.pipe(
      map((r: StrictHttpResponse<OrderResponse>): OrderResponse => r.body)
    );
  }

  static readonly ListOrdersPath = '/orders';

  listOrders$Response(params?: ListOrders$Params, context?: HttpContext): Observable<StrictHttpResponse<Array<OrderResponse>>> {
    const obs = listOrders(this.http, this.rootUrl, params, context);
    return obs;
  }

  listOrders(params?: ListOrders$Params, context?: HttpContext): Observable<Array<OrderResponse>> {
    const resp = this.listOrders$Response(params, context);
    return resp.pipe(
      map((r: StrictHttpResponse<Array<OrderResponse>>): Array<OrderResponse> => r.body)
    );
  }

  static readonly PlaceOrderPath = '/orders';

  placeOrder$Response(params: PlaceOrder$Params, context?: HttpContext): Observable<StrictHttpResponse<OrderResponse>> {
    const obs = placeOrder(this.http, this.rootUrl, params, context);
    return obs;
  }

  placeOrder(params: PlaceOrder$Params, context?: HttpContext): Observable<OrderResponse> {
    const resp = this.placeOrder$Response(params, context);
    return resp.pipe(
      map((r: StrictHttpResponse<OrderResponse>): OrderResponse => r.body)
    );
  }

  static readonly GetOrdersByAccountPath = '/orders/account/{accountId}';

  getOrdersByAccount$Response(params: GetOrdersByAccount$Params, context?: HttpContext): Observable<StrictHttpResponse<Array<OrderResponse>>> {
    const obs = getOrdersByAccount(this.http, this.rootUrl, params, context);
    return obs;
  }

  getOrdersByAccount(params: GetOrdersByAccount$Params, context?: HttpContext): Observable<Array<OrderResponse>> {
    const resp = this.getOrdersByAccount$Response(params, context);
    return resp.pipe(
      map((r: StrictHttpResponse<Array<OrderResponse>>): Array<OrderResponse> => r.body)
    );
  }

}
