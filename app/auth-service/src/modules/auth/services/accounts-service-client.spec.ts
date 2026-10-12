import { jest } from '@jest/globals';
import { Logger, ServiceUnavailableException } from '@nestjs/common';
import { HttpService } from '@nestjs/axios';
import { ConfigService } from '@nestjs/config';
import { AxiosError, AxiosHeaders, AxiosResponse } from 'axios';
import { of, throwError } from 'rxjs';
import { TokenService } from '../../tokens/token.service';
import { AccountsServiceClient } from './accounts-service-client';

const axiosError = (status?: number) =>
  new AxiosError(
    status ? `Request failed with status code ${status}` : 'connect ECONNREFUSED',
    status ? 'ERR_BAD_RESPONSE' : 'ECONNREFUSED',
    { headers: new AxiosHeaders({ Authorization: 'Bearer secret.service.token' }) },
    undefined,
    status ? ({ status, data: {}, headers: {}, statusText: '', config: {} } as unknown as AxiosResponse) : undefined,
  );

describe('AccountsServiceClient', () => {
  const httpService = { post: jest.fn<any>() };
  const tokenService = { issueServiceToken: jest.fn<any>().mockReturnValue('secret.service.token') };
  const config = (url?: string) =>
    ({ get: (name: string, fallback: unknown) => (name === 'ACCOUNTS_SERVICE_URL' && url ? url : fallback) }) as unknown as ConfigService;
  const client = new AccountsServiceClient(
    httpService as unknown as HttpService,
    tokenService as unknown as TokenService,
    config(),
  );
  const created = { accountId: 'ACC0012', userId: 12, holderName: 'Zed Smith', cashBalance: 0, status: 'ACTIVE' };

  beforeEach(() => {
    jest.clearAllMocks();
    jest.spyOn(Logger.prototype, 'warn').mockImplementation(() => undefined);
  });

  describe('createAccount', () => {
    it('asks accounts-service to open an account with just the user ID and holder name', async () => {
      httpService.post.mockReturnValue(of({ data: created }));

      const result = await client.createAccount({ userId: 12, holderName: 'Zed Smith' });

      expect(result).toEqual(created);
      expect(httpService.post).toHaveBeenCalledWith(
        'http://accounts-service:8081/api/accounts',
        { userId: 12, holderName: 'Zed Smith' },
        expect.objectContaining({ timeout: 5000 }),
      );
    });

    it('authenticates with a fresh RS256 service token for auth-service', async () => {
      httpService.post.mockReturnValue(of({ data: created }));

      await client.createAccount({ userId: 12, holderName: 'Zed Smith' });

      expect(tokenService.issueServiceToken).toHaveBeenCalledWith('auth-service');
      expect(httpService.post.mock.calls[0][2]).toMatchObject({ headers: { Authorization: 'Bearer secret.service.token' } });
    });

    it('uses ACCOUNTS_SERVICE_URL when it is set', async () => {
      const custom = new AccountsServiceClient(
        httpService as unknown as HttpService,
        tokenService as unknown as TokenService,
        config('http://localhost:8084'),
      );
      httpService.post.mockReturnValue(of({ data: created }));

      await custom.createAccount({ userId: 12, holderName: 'Zed Smith' });

      expect(httpService.post.mock.calls[0][0]).toBe('http://localhost:8084/api/accounts');
    });

    it('returns the account ID that accounts-service generated', async () => {
      httpService.post.mockReturnValue(of({ data: { ...created, accountId: 'ACC0042' } }));

      expect((await client.createAccount({ userId: 12, holderName: 'Zed Smith' })).accountId).toBe('ACC0042');
    });

    it.each([
      ['accounts-service rejects the token', 401],
      ['accounts-service refuses the request', 403],
      ['accounts-service rejects the body', 422],
      ['accounts-service fails', 500],
      ['accounts-service is down', undefined],
    ])('throws 503 when %s', async (_label, status) => {
      httpService.post.mockReturnValue(throwError(() => axiosError(status)));

      await expect(client.createAccount({ userId: 12, holderName: 'Zed Smith' })).rejects.toThrow(ServiceUnavailableException);
    });

    it('never writes the service token to the logs', async () => {
      const warn = jest.spyOn(Logger.prototype, 'warn').mockImplementation(() => undefined);
      httpService.post.mockReturnValue(throwError(() => axiosError(500)));

      await expect(client.createAccount({ userId: 12, holderName: 'Zed Smith' })).rejects.toThrow();

      expect(warn).toHaveBeenCalledWith(expect.stringContaining('user 12'));
      expect(JSON.stringify(warn.mock.calls)).not.toContain('secret.service.token');
    });
  });
});
