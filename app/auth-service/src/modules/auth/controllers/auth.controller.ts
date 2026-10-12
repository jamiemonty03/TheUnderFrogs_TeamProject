import { Controller, Post, Get, Body, Header, HttpCode, HttpException } from '@nestjs/common';
import { Throttle } from '@nestjs/throttler';
import { AuthService } from '../services/auth.service';
import { ClientCredentialsDto, ClientTokenResponseDto, LoginDto, RegisterDto, RegisterResponseDto, TokenResponseDto } from '../dtos';
import { CurrentUser, Public } from '../../../common/decorators';
import { CurrentUserDto } from '../../../common/dtos/current-user.dto';

@Controller('auth')
export class AuthController {
  constructor(private readonly authService: AuthService) {}

  @Public()
  @Throttle({ default: { limit: 5, ttl: 60000 } }) 
  @Post('register')
  async register(@Body() registerDto: RegisterDto): Promise<RegisterResponseDto> {
    try {
      return await this.authService.register(registerDto);
    } catch (error) {
      if ((error as HttpException).getStatus?.() === 429) {
        throw error;
      }
      throw error;
    }
  }

  @Public()
  @Throttle({ default: { limit: 5, ttl: 60000 } }) 
  @Post('login')
  @HttpCode(200)
  async login(@Body() loginDto: LoginDto): Promise<TokenResponseDto> {
    try {
      return await this.authService.login(loginDto);
    } catch (error) {
      if ((error as HttpException).getStatus?.() === 429) {
        throw error;
      }
      throw error;
    }
  }

  @Public()
  @Throttle({ default: { limit: 10, ttl: 60000 } }) 
  @Post('token')
  @HttpCode(200)
  @Header('Cache-Control', 'no-store')
  async token(@Body() dto: ClientCredentialsDto): Promise<ClientTokenResponseDto> {
    return this.authService.clientCredentials(dto);
  }

  @Get('me')
  async getCurrentUser(@CurrentUser() user: CurrentUserDto): Promise<CurrentUserDto> {
    return user;
  }
}
