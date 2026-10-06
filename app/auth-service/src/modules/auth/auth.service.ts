import { Injectable, UnauthorizedException, HttpException, HttpStatus } from '@nestjs/common';
import { JwtService } from '@nestjs/jwt';
import { ConfigService } from '@nestjs/config';
import { UsersService } from '../users/users.service';
import { AuthRepository } from './auth.repository';
import { LoginDto, RegisterDto, TokenResponseDto, TokenPayloadDto } from './dto';
import { AccountsServiceClient } from './services/accounts-service-client';
import { User } from '../users/entities/user.entity';

@Injectable()
export class AuthService {
  constructor(
    private readonly usersService: UsersService,
    private readonly authRepository: AuthRepository,
    private readonly jwtService: JwtService,
    private readonly configService: ConfigService,
    private readonly accountsServiceClient: AccountsServiceClient,
  ) {}

  register(registerDto: RegisterDto): Promise<TokenResponseDto> {
    return this.usersService.createUser({
      username: registerDto.username,
      email: registerDto.email,
      password: registerDto.password,
      role: 'user',
    }).then(user => {
      return this.accountsServiceClient.createAccount({
        userId: user.id,
        holderName: registerDto.username,
      }).then(account => {
        user.account_id = account.accountId;
        return this.usersService.updateUser(user.id, { account_id: account.accountId })
          .then(() => this.generateToken(user.id, user.username, user.role, account.accountId));
      }).catch(error => {
        return this.usersService.deleteUser(user.id)
          .catch(deleteError => {
            console.error('Failed to rollback user creation:', deleteError);
          })
          .then(() => {
            throw new HttpException(
              {
                error_code: 'REGISTRATION-500',
                message: 'Failed to create trading account. Registration cancelled.',
              },
              HttpStatus.INTERNAL_SERVER_ERROR,
            );
          });
      });
    });
  }

  async login(loginDto: LoginDto): Promise<TokenResponseDto> {
    const user = await this.usersService.getUserByUsername(loginDto.username);

    const isPasswordValid = await this.usersService.validatePassword(
      loginDto.password,
      user.password,
    );

    if (!isPasswordValid) {
      throw new UnauthorizedException('Invalid credentials');
    }

    if (!user.is_active) {
      throw new UnauthorizedException('User account is inactive');
    }

    await this.authRepository.update(user.id, {
      last_login: new Date().toISOString(),
      failed_login_attempts: 0,
    });

    return this.generateToken(user.id, user.username, user.role, user.account_id);
  }


  async validateToken(token: string): Promise<TokenPayloadDto> {
    try {
      return this.jwtService.verify(token);
    } catch {
      throw new UnauthorizedException('Invalid or expired token');
    }
  }

  private generateToken(
    userId: number,
    username: string,
    role: string,
    accountId?: string,
  ): TokenResponseDto {
    const expiresIn = this.configService.get<number>('JWT_EXPIRATION', 86400000);
    const expiresInSeconds = Math.floor(expiresIn / 1000);

    const payload: any = {
      sub: userId,
      username,
      roles: [role],
    };

    if (accountId) {
      payload.accountId = accountId;
    }

    const accessToken = this.jwtService.sign(payload, {
      expiresIn: expiresInSeconds,
    });

    return {
      access_token: accessToken,
      token_type: 'Bearer',
      expires_in: expiresInSeconds,
    };
  }
}
