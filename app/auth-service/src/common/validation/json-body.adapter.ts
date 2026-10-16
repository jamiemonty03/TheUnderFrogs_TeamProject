import { ExpressAdapter } from '@nestjs/platform-express';
import { ValidationException } from '../exceptions/custom.exceptions';

export class JsonBodyAdapter extends ExpressAdapter {
  mapException(error: unknown): unknown {
    if (error instanceof SyntaxError) {
      return new ValidationException('Malformed request body');
    }
    return super.mapException(error);
  }
}
