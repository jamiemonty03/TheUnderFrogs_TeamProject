import { Module } from '@nestjs/common';
import { ConfigService } from '@nestjs/config';
import { TypeOrmModule } from '@nestjs/typeorm';
import { TokensService } from './tokens.service';
import { TokensRepository } from './tokens.repository';
import { Token } from './entities/token.entity';
import { SIGNING_KEY, TokenService } from './token.service';
import { loadSigningKey } from '../../config/signing-key';
import { JwksController } from './jwks.controller';

@Module({
  imports: [TypeOrmModule.forFeature([Token])],
  controllers: [JwksController],
  providers: [
    TokensService,
    TokensRepository,
    TokenService,
    {
      provide: SIGNING_KEY,
      inject: [ConfigService],
      useFactory: (configService: ConfigService) =>
        loadSigningKey(configService.getOrThrow<string>('JWT_PRIVATE_KEY'), configService.getOrThrow<string>('JWT_KEY_ID')),
    },
  ],
  exports: [TokensService, TokensRepository, TokenService, SIGNING_KEY],
})
export class TokensModule {}
