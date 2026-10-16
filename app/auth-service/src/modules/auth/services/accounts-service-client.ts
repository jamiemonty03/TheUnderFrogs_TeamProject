import { Injectable, Logger, ServiceUnavailableException } from '@nestjs/common';
import { HttpService } from '@nestjs/axios';
import { ConfigService } from '@nestjs/config';
import { isAxiosError } from 'axios';
import { firstValueFrom } from 'rxjs';
import { TokenService } from '../../tokens/services/token.service';

export const SERVICE_NAME = 'auth-service';
export const ACCOUNTS_REQUEST_TIMEOUT_MS = 5000;

export interface AccountCreationRequest {
  userId: number;
  holderName: string;
}

export interface AccountResponse {
  accountId: string;
  userId: number;
  holderName: string;
  cashBalance: number;
  status: string;
}

@Injectable()
export class AccountsServiceClient {
  private readonly logger = new Logger(AccountsServiceClient.name);
  private readonly accountsUrl: string;

  constructor(
    private readonly httpService: HttpService,
    private readonly tokenService: TokenService,
    configService: ConfigService,
  ) {
    this.accountsUrl = `${configService.get<string>('ACCOUNTS_SERVICE_URL', 'http://accounts-service:8081')}/api/accounts`;
  }

  async createAccount(request: AccountCreationRequest): Promise<AccountResponse> {
    try {
      const response = await firstValueFrom(
        this.httpService.post<AccountResponse>(this.accountsUrl, request, {
          headers: { Authorization: `Bearer ${this.tokenService.issueServiceToken(SERVICE_NAME)}` },
          timeout: ACCOUNTS_REQUEST_TIMEOUT_MS,
        }),
      );
      return response.data;
    } catch (error) {
      const status = isAxiosError(error) ? error.response?.status : undefined;
      this.logger.warn(
        `Creating an account for user ${request.userId} failed: ${status ?? 'no response'} ${(error as Error).message}`,
      );
      throw new ServiceUnavailableException('Account service is unavailable, please try again');
    }
  }

  async updateHolderName(accountId: string, holderName: string): Promise<void> {
    try {
      await firstValueFrom(
        this.httpService.patch(
          `${this.accountsUrl}/${encodeURIComponent(accountId)}/holder-name`,
          { holderName },
          {
            headers: {
              Authorization: `Bearer ${this.tokenService.issueServiceToken(SERVICE_NAME)}`,
            },
            timeout: ACCOUNTS_REQUEST_TIMEOUT_MS,
          },
        ),
      );
    } catch (error) {
      const status = isAxiosError(error) ? error.response?.status : undefined;
      this.logger.warn(
        `Updating the holder name of account ${accountId} failed: ${status ?? 'no response'} ${(error as Error).message}`,
      );
      throw new ServiceUnavailableException(
        'Account service is unavailable, please try again',
      );
    }
  }
}
