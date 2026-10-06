import { Injectable, UnauthorizedException } from '@nestjs/common';
import { JwtService } from '@nestjs/jwt';
import { ConfigService } from '@nestjs/config';
import { UsersService } from '../users/users.service';
import { AuthRepository } from './auth.repository';
import { LoginDto, RegisterDto, TokenResponseDto, TokenPayloadDto } from './dto';

@Injectable()
export class AuthService {
  constructor(
    private readonly usersService: UsersService,
    private readonly authRepository: AuthRepository,
    private readonly jwtService: JwtService,
    private readonly configService: ConfigService,
  ) {}

  async register(registerDto: RegisterDto): Promise<TokenResponseDto> {
    const user = await this.usersService.createUser({
      username: registerDto.username,
      email: registerDto.email,
      password: registerDto.password,
      role: 'USER',
    });

    return this.generateToken(user.id, user.username, user.roles);
  }

  async login(loginDto: LoginDto): Promise<TokenResponseDto> {
    const user = await this.usersService.getUserByUsername(loginDto.username);

    const isPasswordValid = await this.usersService.validatePassword(
      loginDto.password,
      user.password_hash,
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

    return this.generateToken(user.id, user.username, user.roles);
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
    roles: string[],
  ): TokenResponseDto {
    const expiresIn = this.configService.get<number>('JWT_EXPIRATION', 86400000);
    const expiresInSeconds = Math.floor(expiresIn / 1000);

    const payload = {
      sub: userId,
      username,
      roles,
    };

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
