import { Injectable, Logger, ServiceUnavailableException, UnauthorizedException } from '@nestjs/common';
import { UsersService } from '../../users/services/users.service';
import { AuthRepository } from '../repositories/auth.repository';
import { ClientCredentialsDto, ClientTokenResponseDto, LoginDto, RegisterDto, RegisterResponseDto, TokenResponseDto } from '../dtos';
import { ServiceClientsService } from '../../clients/services/service-clients.service';
import { DEFAULT_ROLE } from '../../users/dtos';
import { AccessTokenClaims, TokenService } from '../../tokens/services/token.service';
import { RefreshTokensService } from '../../tokens/services/refresh-tokens.service';
import { AccountsServiceClient } from './accounts-service-client';
import { User } from '../../users/entities/user.entity';

export const REGISTRATION_FAILED = 'Registration could not be completed, please try again';

@Injectable()
export class AuthService {
  private readonly logger = new Logger(AuthService.name);

  constructor(
    private readonly usersService: UsersService,
    private readonly authRepository: AuthRepository,
    private readonly tokenService: TokenService,
    private readonly refreshTokensService: RefreshTokensService,
    private readonly accountsServiceClient: AccountsServiceClient,
    private readonly serviceClientsService: ServiceClientsService,
  ) {}

  async register(registerDto: RegisterDto): Promise<RegisterResponseDto> {
    const user = await this.usersService.createUser({
      username: registerDto.username,
      email: registerDto.email,
      password: registerDto.password,
      full_name: registerDto.full_name,
      role: DEFAULT_ROLE,
    });

    let accountId: string;
    try {
      ({ accountId } = await this.accountsServiceClient.createAccount({
        userId: user.id,
        holderName: user.full_name ?? user.username,
      }));
    } catch {
      await this.removeUnfinishedUser(user.id);
      throw new ServiceUnavailableException(REGISTRATION_FAILED);
    }

    let linked: User;
    try {
      linked = await this.usersService.linkAccount(user.id, accountId);
    } catch (error) {
      this.logger.error(`Account ${accountId} was created but could not be linked to user ${user.id}: ${(error as Error).message}`);
      await this.removeUnfinishedUser(user.id);
      throw new ServiceUnavailableException(REGISTRATION_FAILED);
    }

    return { ...(await this.issueTokens(linked)), accountId };
  }

  async login(loginDto: LoginDto): Promise<TokenResponseDto> {
    const user = await this.usersService.getUserByUsername(loginDto.username);
    if (user && user.locked_until && user.locked_until > new Date()) {
      this.logger.warn(`Login attempt on locked account (ID: ${user.id})`);
      throw new UnauthorizedException('Account is locked due to too many failed login attempts. Please try again later.');
    }

    const authenticatedUser = await this.usersService.verifyCredentials(loginDto.username, loginDto.password);

    if (!authenticatedUser || !authenticatedUser.is_active) {
      if (user) {
        await this.usersService.recordFailedLogin(user.id);
        this.logger.warn(`Failed login attempt for user ID: ${user.id}`);
      } else {
        this.logger.warn(`Login attempt with non-existent or inactive user`);
      }
      throw new UnauthorizedException('Invalid credentials');
    }

    await this.usersService.recordSuccessfulLogin(authenticatedUser.id);
    this.logger.log(`Successful login for user ID: ${authenticatedUser.id}`);

    await this.authRepository.recordLogin(authenticatedUser.id);

    return this.issueTokens(authenticatedUser);
  }


  async clientCredentials(dto: ClientCredentialsDto): Promise<ClientTokenResponseDto> {
    const client = await this.serviceClientsService.verify(dto.client_id, dto.client_secret);
    if (!client) {
      throw new UnauthorizedException('Invalid client credentials');
    }
    const { accessToken, expiresIn } = this.tokenService.issueClientToken(client.client_id, client.roles);
    return { access_token: accessToken, token_type: 'Bearer', expires_in: expiresIn };
  }

  async validateToken(token: string): Promise<AccessTokenClaims> {
    return this.tokenService.verify(token);
  }

  private async removeUnfinishedUser(userId: number): Promise<void> {
    try {
      await this.usersService.deleteUser(userId);
    } catch (error) {
      this.logger.error(`Could not remove user ${userId} after a failed registration: ${(error as Error).message}`);
    }
  }

  private async issueTokens(user: User): Promise<TokenResponseDto> {
    const { accessToken, expiresIn } = this.tokenService.issue(user);
    const { refreshToken } = await this.refreshTokensService.issue(user.id);
    return { accessToken, refreshToken, expiresIn, mfaRequired: false };
  }
}
