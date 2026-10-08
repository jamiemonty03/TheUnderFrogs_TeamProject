import { Injectable, UnauthorizedException, HttpException, HttpStatus } from '@nestjs/common';
import { UsersService } from '../users/users.service';
import { AuthRepository } from './auth.repository';
import { LoginDto, RegisterDto, TokenResponseDto } from './dto';
import { DEFAULT_ROLE } from '../users/dto';
import { AccessTokenClaims, TokenService } from '../tokens/token.service';
import { RefreshTokensService } from '../tokens/refresh-tokens.service';
import { AccountsServiceClient } from './services/accounts-service-client';
import { User } from '../users/entities/user.entity';

@Injectable()
export class AuthService {
  constructor(
    private readonly usersService: UsersService,
    private readonly authRepository: AuthRepository,
    private readonly tokenService: TokenService,
    private readonly refreshTokensService: RefreshTokensService,
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

    return this.issueTokens(user);
  }

  async login(loginDto: LoginDto): Promise<TokenResponseDto> {
    const user = await this.usersService.verifyCredentials(loginDto.username, loginDto.password);

    if (!user || !user.is_active) {
      throw new UnauthorizedException('Invalid credentials');
    }

    await this.authRepository.recordLogin(user.id);

    return this.issueTokens(user);
  }


  async validateToken(token: string): Promise<AccessTokenClaims> {
    return this.tokenService.verify(token);
  }

  private async issueTokens(user: User): Promise<TokenResponseDto> {
    const { accessToken, expiresIn } = this.tokenService.issue(user);
    const { refreshToken } = await this.refreshTokensService.issue(user.id);
    return { accessToken, refreshToken, expiresIn, mfaRequired: false };
  }
}
