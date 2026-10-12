import { Module } from '@nestjs/common';
import { ConfigService } from '@nestjs/config';
import { TypeOrmModule } from '@nestjs/typeorm';
import { RefreshTokensService } from './services/refresh-tokens.service';
import { RefreshTokensRepository } from './repositories/refresh-tokens.repository';
import { RefreshToken } from './entities/refresh-token.entity';
import { SIGNING_KEY, TokenService } from './services/token.service';
import { loadSigningKey } from '../../config/signing-key';
import { JwksController } from './controllers/jwks.controller';

@Module({
  imports: [TypeOrmModule.forFeature([RefreshToken])],
  controllers: [JwksController],
  providers: [
    RefreshTokensService,
    RefreshTokensRepository,
    TokenService,
    {
      provide: SIGNING_KEY,
      inject: [ConfigService],
      useFactory: (configService: ConfigService) =>
        loadSigningKey(configService.getOrThrow<string>('JWT_PRIVATE_KEY'), configService.getOrThrow<string>('JWT_KEY_ID')),
    },
  ],
  exports: [RefreshTokensService, TokenService, SIGNING_KEY],
})
export class TokensModule {}
