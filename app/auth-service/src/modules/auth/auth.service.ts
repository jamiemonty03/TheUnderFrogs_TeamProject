import { Injectable, UnauthorizedException, HttpException, HttpStatus } from '@nestjs/common';
import { UsersService } from '../users/users.service';
import { AuthRepository } from './auth.repository';
import { LoginDto, RegisterDto, TokenResponseDto } from './dto';
import { DEFAULT_ROLE } from '../users/dto';
import { AccessTokenClaims, TokenService } from '../tokens/token.service';
import { AccountsServiceClient } from './services/accounts-service-client';
import { User } from '../users/entities/user.entity';

@Injectable()
export class AuthService {
  constructor(
    private readonly usersService: UsersService,
    private readonly authRepository: AuthRepository,
    private readonly tokenService: TokenService,
    private readonly accountsServiceClient: AccountsServiceClient,
  ) {}

  async register(registerDto: RegisterDto): Promise<TokenResponseDto> {
    const user = await this.usersService.createUser({
      username: registerDto.username,
      email: registerDto.email,
      password: registerDto.password,
      full_name: registerDto.full_name,
      role: DEFAULT_ROLE,
    });

    return this.generateToken(user);
  }

  async login(loginDto: LoginDto): Promise<TokenResponseDto> {
    const user = await this.usersService.verifyCredentials(loginDto.username, loginDto.password);

    if (!user) {
      throw new UnauthorizedException('Invalid credentials');
    }

    if (!user.is_active) {
      throw new UnauthorizedException('User account is inactive');
    }

    await this.authRepository.update(user.id, {
      last_login: new Date().toISOString(),
      failed_login_attempts: 0,
    });

    return this.generateToken(user);
  }


  async validateToken(token: string): Promise<AccessTokenClaims> {
    return this.tokenService.verify(token);
  }

  private generateToken(user: User): TokenResponseDto {
    const { accessToken, expiresIn } = this.tokenService.issue(user);
    return {
      access_token: accessToken,
      token_type: 'Bearer',
      expires_in: expiresIn,
    };
  }
}
