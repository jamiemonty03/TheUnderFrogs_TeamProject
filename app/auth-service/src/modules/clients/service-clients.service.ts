import { Injectable } from '@nestjs/common';
import { ServiceClientsRepository } from './service-clients.repository';
import { ServiceClient } from './entities/service-client.entity';
import { PasswordHasher } from '../users/password-hasher.service';

@Injectable()
export class ServiceClientsService {
  private dummyHash?: Promise<string>;

  constructor(
    private readonly serviceClientsRepository: ServiceClientsRepository,
    private readonly passwordHasher: PasswordHasher,
  ) {}

  async verify(clientId: string, clientSecret: string): Promise<ServiceClient | null> {
    const client = await this.serviceClientsRepository.findActiveByClientId(clientId);
    if (!client) {
      await this.passwordHasher.verify(clientSecret, await this.getDummyHash());
      return null;
    }
    return (await this.passwordHasher.verify(clientSecret, client.client_secret_hash)) ? client : null;
  }

  private getDummyHash(): Promise<string> {
    this.dummyHash ??= this.passwordHasher.hash('dummy-client-secret-for-timing');
    return this.dummyHash;
  }
}
