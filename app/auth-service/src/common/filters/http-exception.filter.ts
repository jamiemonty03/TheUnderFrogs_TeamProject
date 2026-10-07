import { ArgumentsHost, Catch, ExceptionFilter, HttpException, HttpStatus, Logger } from '@nestjs/common';
import { Response } from 'express';
import { ErrorResponseDto } from '../dto/error-response.dto';


@Catch()
export class HttpExceptionFilter implements ExceptionFilter {
  private readonly logger = new Logger(HttpExceptionFilter.name);

  catch(exception: unknown, host: ArgumentsHost) {
    const res = host.switchToHttp().getResponse<Response>();

    if (!(exception instanceof HttpException)) {
      this.logger.error('Unexpected error', exception instanceof Error ? exception.stack : String(exception));
      const body: ErrorResponseDto = { errorCode: 'SRV-500', message: 'Unexpected error' };
      res.status(HttpStatus.INTERNAL_SERVER_ERROR).json(body);
      return;
    }

    const status = exception.getStatus();
    const body: ErrorResponseDto = {
      errorCode: `${status === HttpStatus.UNPROCESSABLE_ENTITY ? 'VAL' : 'AUTH'}-${status}`,
      message: this.extractMessage(exception),
    };
    res.status(status).json(body);
  }

  private extractMessage(exception: HttpException): string {
    const response = exception.getResponse();
    if (typeof response === 'string') return response;
    const message = (response as { message?: string | string[] }).message;
    if (Array.isArray(message)) return message[0] ?? exception.message;
    return message ?? exception.message;
  }
}
