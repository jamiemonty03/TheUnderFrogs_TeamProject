import { createParamDecorator, ExecutionContext } from '@nestjs/common';
import { CurrentUserDto } from '../dto/current-user.dto';

export const CurrentUser = createParamDecorator(
  (data: unknown, ctx: ExecutionContext): CurrentUserDto => {
    const request = ctx.switchToHttp().getRequest();
    const user = request.user;

    return {
      userId: Number(user.sub),
      username: user.username,
      roles: user.roles || [],
      accountId: user.accountId,
    };
  },
);
