import { Controller, Post, Get, Body, HttpCode } from '@nestjs/common';
import { AuthService } from './auth.service';
import { LoginDto, RegisterDto, TokenResponseDto } from './dto';
import { CurrentUser, Public } from '../../common/decorators';
import { CurrentUserDto } from '../../common/dto/current-user.dto';

@Controller('auth')
export class AuthController {
  constructor(private readonly authService: AuthService) {}

  @Public()
  @Post('register')
  async register(@Body() registerDto: RegisterDto): Promise<TokenResponseDto> {
    return this.authService.register(registerDto);
  }

  @Public()
  @Post('login')
  @HttpCode(200)
  async login(@Body() loginDto: LoginDto): Promise<TokenResponseDto> {
    return this.authService.login(loginDto);
  }

  @Get('me')
  async getCurrentUser(@CurrentUser() user: CurrentUserDto): Promise<CurrentUserDto> {
    return user;
  }
}
