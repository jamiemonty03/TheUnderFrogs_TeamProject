import { Module } from '@nestjs/common';
import { TypeOrmModule } from '@nestjs/typeorm';
import { HttpModule } from '@nestjs/axios';
import { APP_GUARD } from '@nestjs/core';
import { ThrottlerGuard } from '@nestjs/throttler';
import { AuthService } from './services/auth.service';
import { AuthController } from './controllers/auth.controller';
import { AuthRepository } from './repositories/auth.repository';
import { Auth } from './entities/auth.entity';
import { UsersModule } from '../users/users.module';
import { TokensModule } from '../tokens/tokens.module';
import { ClientsModule } from '../clients/clients.module';
import { JwtAuthGuard, RolesGuard } from '../../common/guards';
import { AccountsServiceClient } from './services/accounts-service-client';

@Module({
  imports: [
    TypeOrmModule.forFeature([Auth]),
    HttpModule,
    UsersModule,
    TokensModule,
    ClientsModule,
  ],
  controllers: [AuthController],
  providers: [
    AuthService,
    AuthRepository,
    AccountsServiceClient,
    {
      provide: APP_GUARD,
      useClass: ThrottlerGuard,
    },
    {
      provide: APP_GUARD,
      useClass: JwtAuthGuard,
    },
    {
      provide: APP_GUARD,
      useClass: RolesGuard,
    },
  ],
  exports: [AuthService],
})
export class AuthModule {}
