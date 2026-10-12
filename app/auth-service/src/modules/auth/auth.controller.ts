import { Controller, Post, Get, Body, Header, HttpCode } from '@nestjs/common';
import { AuthService } from './auth.service';
import { ClientCredentialsDto, ClientTokenResponseDto, LoginDto, RegisterDto, RegisterResponseDto, TokenResponseDto } from './dto';
import { CurrentUser, Public } from '../../common/decorators';
import { CurrentUserDto } from '../../common/dto/current-user.dto';

@Controller('auth')
export class AuthController {
  constructor(private readonly authService: AuthService) {}

  @Public()
  @Post('register')
  async register(@Body() registerDto: RegisterDto): Promise<RegisterResponseDto> {
    return this.authService.register(registerDto);
  }

  @Public()
  @Post('login')
  @HttpCode(200)
  async login(@Body() loginDto: LoginDto): Promise<TokenResponseDto> {
    return this.authService.login(loginDto);
  }

  @Public()
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
