import { Controller, Get, Header } from '@nestjs/common';
import { Gauge, register } from 'prom-client';
import { Public } from './common/decorators/public.decorator';
import { RefreshTokensRepository } from './modules/tokens/repositories/refresh-tokens.repository';

@Controller('metrics')
export class MetricsController {
  private readonly activeUsers: Gauge<string>;

  constructor(private readonly refreshTokensRepository: RefreshTokensRepository) {
    this.activeUsers = (register.getSingleMetric('auth_active_users') as Gauge<string> | undefined)
      ?? new Gauge({
        name: 'auth_active_users',
        help: 'Distinct users with a non-revoked, unexpired refresh-token session',
        registers: [register],
      });
  }

  @Public()
  @Get()
  @Header('Content-Type', register.contentType)
  async getMetrics(): Promise<string> {
    this.activeUsers.set(await this.refreshTokensRepository.countActiveUsers());
    return register.metrics();
  }
}
