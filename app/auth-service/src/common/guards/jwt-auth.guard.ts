import {
  Injectable,
  CanActivate,
  ExecutionContext,
  UnauthorizedException,
} from '@nestjs/common';
import { Reflector } from '@nestjs/core';
import { TokenService } from '../../modules/tokens/token.service';
import { IS_PUBLIC_KEY } from '../decorators/public.decorator';

@Injectable()
export class JwtAuthGuard implements CanActivate {
  constructor(
    private readonly tokenService: TokenService,
    private readonly reflector: Reflector,
  ) {}

  canActivate(context: ExecutionContext): boolean {
    const isPublic = this.reflector.getAllAndOverride<boolean>(IS_PUBLIC_KEY, [
      context.getHandler(),
      context.getClass(),
    ]);

    if (isPublic) {
      return true;
    }

    const request = context.switchToHttp().getRequest();
    const token = this.extractToken(request);

    if (!token) {
      throw new UnauthorizedException({
        error_code: 'AUTH-401',
        message: 'Missing authentication token',
      });
    }

    try {
      const payload = this.tokenService.verify(token);
      request.user = payload;
      return true;
    } catch {
      throw new UnauthorizedException({
        error_code: 'AUTH-401',
        message: 'Invalid or expired token',
      });
    }
  }

  private extractToken(request: any): string | null {
    const authHeader = request.headers.authorization;

    if (!authHeader) {
      return null;
    }

    const [scheme, token] = authHeader.split(' ');

    if (scheme !== 'Bearer' || !token) {
      return null;
    }

    return token;
  }
}
