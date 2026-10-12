import { HttpClient, HttpContext } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';

import { BaseService } from '../base-service';
import { ApiConfiguration } from '../api-configuration';
import { StrictHttpResponse } from '../strict-http-response';

import { deleteInstrument } from '../fn/instruments/delete-instrument';
import { DeleteInstrument$Params } from '../fn/instruments/delete-instrument';
import { getInstrument } from '../fn/instruments/get-instrument';
import { GetInstrument$Params } from '../fn/instruments/get-instrument';
import { getInstruments } from '../fn/instruments/get-instruments';
import { GetInstruments$Params } from '../fn/instruments/get-instruments';
import { InstrumentResponse } from '../models/instrument-response';

@Injectable({ providedIn: 'root' })
export class InstrumentsService extends BaseService {
  constructor(config: ApiConfiguration, http: HttpClient) {
    super(config, http);
  }

  static readonly GetInstrumentsPath = '/instruments';

  getInstruments$Response(params?: GetInstruments$Params, context?: HttpContext): Observable<StrictHttpResponse<Array<InstrumentResponse>>> {
    const obs = getInstruments(this.http, this.rootUrl, params, context);
    return obs;
  }

  getInstruments(params?: GetInstruments$Params, context?: HttpContext): Observable<Array<InstrumentResponse>> {
    const resp = this.getInstruments$Response(params, context);
    return resp.pipe(
      map((r: StrictHttpResponse<Array<InstrumentResponse>>): Array<InstrumentResponse> => r.body)
    );
  }

  static readonly GetInstrumentPath = '/instruments/{symbol}';

  getInstrument$Response(params: GetInstrument$Params, context?: HttpContext): Observable<StrictHttpResponse<InstrumentResponse>> {
    const obs = getInstrument(this.http, this.rootUrl, params, context);
    return obs;
  }

  getInstrument(params: GetInstrument$Params, context?: HttpContext): Observable<InstrumentResponse> {
    const resp = this.getInstrument$Response(params, context);
    return resp.pipe(
      map((r: StrictHttpResponse<InstrumentResponse>): InstrumentResponse => r.body)
    );
  }

  static readonly DeleteInstrumentPath = '/instruments/{symbol}';

  deleteInstrument$Response(params: DeleteInstrument$Params, context?: HttpContext): Observable<StrictHttpResponse<void>> {
    const obs = deleteInstrument(this.http, this.rootUrl, params, context);
    return obs;
  }

  deleteInstrument(params: DeleteInstrument$Params, context?: HttpContext): Observable<void> {
    const resp = this.deleteInstrument$Response(params, context);
    return resp.pipe(
      map((r: StrictHttpResponse<void>): void => r.body)
    );
  }

}
