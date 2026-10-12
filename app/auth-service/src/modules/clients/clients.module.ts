import { Module } from '@nestjs/common';
import { TypeOrmModule } from '@nestjs/typeorm';
import { ServiceClient } from './entities/service-client.entity';
import { ServiceClientsRepository } from './service-clients.repository';

@Module({
  imports: [TypeOrmModule.forFeature([ServiceClient])],
  providers: [ServiceClientsRepository],
  exports: [ServiceClientsRepository],
})
export class ClientsModule {}
