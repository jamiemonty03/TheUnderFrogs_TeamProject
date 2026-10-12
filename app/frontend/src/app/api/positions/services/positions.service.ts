import { HttpClient, HttpContext } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';

import { BaseService } from '../base-service';
import { ApiConfiguration } from '../api-configuration';
import { StrictHttpResponse } from '../strict-http-response';

import { buyPosition } from '../fn/positions/buy-position';
import { BuyPosition$Params } from '../fn/positions/buy-position';
import { createPosition } from '../fn/positions/create-position';
import { CreatePosition$Params } from '../fn/positions/create-position';
import { deletePosition } from '../fn/positions/delete-position';
import { DeletePosition$Params } from '../fn/positions/delete-position';
import { partiallyUpdatePosition } from '../fn/positions/partially-update-position';
import { PartiallyUpdatePosition$Params } from '../fn/positions/partially-update-position';
import { Position } from '../models/position';
import { reversePosition } from '../fn/positions/reverse-position';
import { ReversePosition$Params } from '../fn/positions/reverse-position';
import { sellPosition } from '../fn/positions/sell-position';
import { SellPosition$Params } from '../fn/positions/sell-position';
import { updatePosition } from '../fn/positions/update-position';
import { UpdatePosition$Params } from '../fn/positions/update-position';

@Injectable({ providedIn: 'root' })
export class PositionsService extends BaseService {
  constructor(config: ApiConfiguration, http: HttpClient) {
    super(config, http);
  }

  static readonly CreatePositionPath = '/positions';

  createPosition$Response(params: CreatePosition$Params, context?: HttpContext): Observable<StrictHttpResponse<Position>> {
    const obs = createPosition(this.http, this.rootUrl, params, context);
    return obs;
  }

  createPosition(params: CreatePosition$Params, context?: HttpContext): Observable<Position> {
    const resp = this.createPosition$Response(params, context);
    return resp.pipe(
      map((r: StrictHttpResponse<Position>): Position => r.body)
    );
  }

  static readonly UpdatePositionPath = '/positions/{accountId}/{symbol}';

  updatePosition$Response(params: UpdatePosition$Params, context?: HttpContext): Observable<StrictHttpResponse<Position>> {
    const obs = updatePosition(this.http, this.rootUrl, params, context);
    return obs;
  }

  updatePosition(params: UpdatePosition$Params, context?: HttpContext): Observable<Position> {
    const resp = this.updatePosition$Response(params, context);
    return resp.pipe(
      map((r: StrictHttpResponse<Position>): Position => r.body)
    );
  }

  static readonly DeletePositionPath = '/positions/{accountId}/{symbol}';

  deletePosition$Response(params: DeletePosition$Params, context?: HttpContext): Observable<StrictHttpResponse<void>> {
    const obs = deletePosition(this.http, this.rootUrl, params, context);
    return obs;
  }

  deletePosition(params: DeletePosition$Params, context?: HttpContext): Observable<void> {
    const resp = this.deletePosition$Response(params, context);
    return resp.pipe(
      map((r: StrictHttpResponse<void>): void => r.body)
    );
  }

  static readonly PartiallyUpdatePositionPath = '/positions/{accountId}/{symbol}';

  partiallyUpdatePosition$Response(params: PartiallyUpdatePosition$Params, context?: HttpContext): Observable<StrictHttpResponse<Position>> {
    const obs = partiallyUpdatePosition(this.http, this.rootUrl, params, context);
    return obs;
  }

  partiallyUpdatePosition(params: PartiallyUpdatePosition$Params, context?: HttpContext): Observable<Position> {
    const resp = this.partiallyUpdatePosition$Response(params, context);
    return resp.pipe(
      map((r: StrictHttpResponse<Position>): Position => r.body)
    );
  }

  static readonly BuyPositionPath = '/positions/{accountId}/{symbol}/buy';

  buyPosition$Response(params: BuyPosition$Params, context?: HttpContext): Observable<StrictHttpResponse<Position>> {
    const obs = buyPosition(this.http, this.rootUrl, params, context);
    return obs;
  }

  buyPosition(params: BuyPosition$Params, context?: HttpContext): Observable<Position> {
    const resp = this.buyPosition$Response(params, context);
    return resp.pipe(
      map((r: StrictHttpResponse<Position>): Position => r.body)
    );
  }

  static readonly SellPositionPath = '/positions/{accountId}/{symbol}/sell';

  sellPosition$Response(params: SellPosition$Params, context?: HttpContext): Observable<StrictHttpResponse<Position>> {
    const obs = sellPosition(this.http, this.rootUrl, params, context);
    return obs;
  }

  sellPosition(params: SellPosition$Params, context?: HttpContext): Observable<Position> {
    const resp = this.sellPosition$Response(params, context);
    return resp.pipe(
      map((r: StrictHttpResponse<Position>): Position => r.body)
    );
  }

  static readonly ReversePositionPath = '/positions/{accountId}/{symbol}/reversal';

  reversePosition$Response(params: ReversePosition$Params, context?: HttpContext): Observable<StrictHttpResponse<Position>> {
    const obs = reversePosition(this.http, this.rootUrl, params, context);
    return obs;
  }

  reversePosition(params: ReversePosition$Params, context?: HttpContext): Observable<Position> {
    const resp = this.reversePosition$Response(params, context);
    return resp.pipe(
      map((r: StrictHttpResponse<Position>): Position => r.body)
    );
  }

}
