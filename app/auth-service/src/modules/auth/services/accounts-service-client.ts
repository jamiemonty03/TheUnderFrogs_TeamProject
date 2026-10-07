import { Injectable, HttpException, HttpStatus } from '@nestjs/common';
import { HttpService } from '@nestjs/axios';
import { ConfigService } from '@nestjs/config';
import { JwtService } from '@nestjs/jwt';
import { firstValueFrom } from 'rxjs';

export interface AccountCreationRequest {
  userId: number;
  holderName: string;
  accountId?: string;
}

export interface AccountResponse {
  accountId: string;
  userId: number;
  holderName: string;
  cashBalance: string;
  status: string;
  createdAt: string;
}

@Injectable()
export class AccountsServiceClient {
  private readonly accountsServiceUrl: string;
  private readonly jwtSecret: string;

  constructor(
    private readonly httpService: HttpService,
    private readonly configService: ConfigService,
    private readonly jwtService: JwtService,
  ) {
    this.accountsServiceUrl = this.configService.get<string>(
      'ACCOUNTS_SERVICE_URL',
      'http://accounts-service:8081',
    );
    this.jwtSecret = this.configService.get<string>('JWT_SECRET', '');
  }

  private generateServiceToken(): string {
    return this.jwtService.sign(
      { roles: ['SERVICE'] },
      {
        subject: 'auth-service',
        secret: this.jwtSecret,
        expiresIn: '1h',
      },
    );
  }

  async createAccount(
    request: AccountCreationRequest,
  ): Promise<AccountResponse> {
    try {
      const serviceToken = this.generateServiceToken();
      const headers: any = {
        'Content-Type': 'application/json',
        Authorization: `Bearer ${serviceToken}`,
      };

      // Generate accountId if not provided
      const accountId =
        request.accountId || `ACC-${String(request.userId).padStart(6, '0')}`;

      const res = await firstValueFrom(
        this.httpService.post<AccountResponse>(
          `${this.accountsServiceUrl}/api/accounts`,
          {
            accountId,
            userId: request.userId,
            holderName: request.holderName,
            cashBalance: 0,
            status: 'ACTIVE',
          },
          { headers },
        ),
      );

      return (res as any).data;
    } catch (error: any) {
      console.error('Account creation error:', {
        status: error?.response?.status,
        data: error?.response?.data,
        message: error?.message,
        fullError: error,
      });
      
      if (error?.response?.status === 409) {
        throw new HttpException(
          {
            error_code: 'ACCOUNT-409',
            message: 'Account already exists',
          },
          HttpStatus.CONFLICT,
        );
      }

      throw new HttpException(
        {
          error_code: 'ACCOUNT-500',
          message: `Failed to create account: ${error?.message || 'Unknown error'}`,
        },
        HttpStatus.INTERNAL_SERVER_ERROR,
      );
    }
  }

  async getAccountByUserId(userId: number): Promise<AccountResponse | null> {
    try {
      const serviceToken = this.generateServiceToken();
      const headers: any = {
        Authorization: `Bearer ${serviceToken}`,
      };

      const res = await firstValueFrom(
        this.httpService.get<AccountResponse[]>(
          `${this.accountsServiceUrl}/api/accounts?userId=${userId}`,
          { headers },
        ),
      );

      return ((res as any).data[0] || null);
    } catch (error: any) {
      if (error?.response?.status === 404) {
        return null;
      }

      throw new HttpException(
        {
          error_code: 'ACCOUNT-500',
          message: `Failed to fetch account: ${error?.message || 'Unknown error'}`,
        },
        HttpStatus.INTERNAL_SERVER_ERROR,
      );
    }
  }
}
