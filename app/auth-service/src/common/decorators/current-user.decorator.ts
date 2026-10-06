import { createParamDecorator, ExecutionContext } from '@nestjs/common';
import { CurrentUserDto } from '../dto/current-user.dto';

export const CurrentUser = createParamDecorator(
  (data: unknown, ctx: ExecutionContext): CurrentUserDto => {
    const request = ctx.switchToHttp().getRequest();
    const user = request.user;

    return {
      userId: user.sub,
      username: user.username,
      roles: user.roles || [],
      accountId: user.accountId, // This will be undefined for now but ready for future integration
    };
  },
);
