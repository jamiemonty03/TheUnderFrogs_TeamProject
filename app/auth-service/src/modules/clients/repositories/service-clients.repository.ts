import { Injectable } from '@nestjs/common';
import { InjectRepository } from '@nestjs/typeorm';
import { Repository } from 'typeorm';
import { ServiceClient } from '../entities/service-client.entity';

@Injectable()
export class ServiceClientsRepository {
  constructor(
    @InjectRepository(ServiceClient)
    private readonly repository: Repository<ServiceClient>,
  ) {}

  findActiveByClientId(clientId: string): Promise<ServiceClient | null> {
    return this.repository.findOne({ where: { client_id: clientId, is_active: true } });
  }
}
