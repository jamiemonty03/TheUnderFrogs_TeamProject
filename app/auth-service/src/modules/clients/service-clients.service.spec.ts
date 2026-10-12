import { jest } from '@jest/globals';
import { ServiceClientsService } from './service-clients.service';
import { ServiceClientsRepository } from './service-clients.repository';
import { ServiceClient } from './entities/service-client.entity';
import { PasswordHasher } from '../users/password-hasher.service';

describe('ServiceClientsService', () => {
  const hasher = new PasswordHasher();
  let stored: ServiceClient | null;
  const repository = { findActiveByClientId: jest.fn(async (id: string) => (stored?.client_id === id ? stored : null)) };
  const service = new ServiceClientsService(repository as unknown as ServiceClientsRepository, hasher);

  beforeAll(async () => {
    stored = {
      id: 1,
      client_id: 'trade-executor',
      client_secret_hash: await hasher.hash('the-right-client-secret'),
      roles: ['SERVICE'],
      is_active: true,
    } as ServiceClient;
  });

  afterEach(() => jest.restoreAllMocks());

  it('returns the client when the secret is right', async () => {
    expect((await service.verify('trade-executor', 'the-right-client-secret'))?.client_id).toBe('trade-executor');
  });

  it('returns null for a wrong secret', async () => {
    expect(await service.verify('trade-executor', 'a-wrong-client-secret')).toBeNull();
  });

  it('returns null for an unknown or inactive client and still runs a hash check, so timing does not reveal client IDs', async () => {
    const verify = jest.spyOn(hasher, 'verify');

    expect(await service.verify('not-a-client', 'whatever')).toBeNull();
    expect(verify).toHaveBeenCalledTimes(1);
    expect(verify.mock.calls[0][1]).toMatch(/^\$argon2id\$/);
  });

  it('never accepts the stored hash itself as the secret', async () => {
    expect(await service.verify('trade-executor', stored!.client_secret_hash)).toBeNull();
  });
});
