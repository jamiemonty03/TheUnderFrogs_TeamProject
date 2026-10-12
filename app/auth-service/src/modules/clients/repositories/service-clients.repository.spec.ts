import { jest } from '@jest/globals';
import { Repository } from 'typeorm';
import { ServiceClientsRepository } from './service-clients.repository';
import { ServiceClient } from '../entities/service-client.entity';

describe('ServiceClientsRepository', () => {
  const typeorm = { findOne: jest.fn<any>() };
  const repository = new ServiceClientsRepository(typeorm as unknown as Repository<ServiceClient>);

  beforeEach(() => jest.clearAllMocks());

  it('looks a client up by its ID, ignoring clients that are switched off', async () => {
    const client = { client_id: 'trade-executor', is_active: true } as ServiceClient;
    typeorm.findOne.mockResolvedValue(client);

    await expect(repository.findActiveByClientId('trade-executor')).resolves.toBe(client);
    expect(typeorm.findOne).toHaveBeenCalledWith({ where: { client_id: 'trade-executor', is_active: true } });
  });

  it('returns null for an unknown or inactive client', async () => {
    typeorm.findOne.mockResolvedValue(null);

    await expect(repository.findActiveByClientId('nobody')).resolves.toBeNull();
  });
});
