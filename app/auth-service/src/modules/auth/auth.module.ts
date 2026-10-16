import { Module } from '@nestjs/common';
import { TypeOrmModule } from '@nestjs/typeorm';
import { HttpModule } from '@nestjs/axios';
import { APP_GUARD } from '@nestjs/core';
import { ThrottlerGuard } from '@nestjs/throttler';
import { AuthService } from './services/auth.service';
import { AuthController } from './controllers/auth.controller';
import { ProfileController } from './controllers/profile.controller';
import { ProfileService } from './services/profile.service';
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
  controllers: [AuthController, ProfileController],
  providers: [
    AuthService,
    AuthRepository,
    AccountsServiceClient,
    ProfileService,
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
