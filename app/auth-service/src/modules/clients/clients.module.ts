import { Module } from '@nestjs/common';
import { TypeOrmModule } from '@nestjs/typeorm';
import { ServiceClient } from './entities/service-client.entity';
import { ServiceClientsRepository } from './service-clients.repository';
import { ServiceClientsService } from './service-clients.service';
import { UsersModule } from '../users/users.module';

@Module({
  imports: [TypeOrmModule.forFeature([ServiceClient]), UsersModule],
  providers: [ServiceClientsRepository, ServiceClientsService],
  exports: [ServiceClientsService],
})
export class ClientsModule {}
