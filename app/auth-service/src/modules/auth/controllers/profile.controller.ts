import {
  Body,
  Controller,
  Get,
  Header,
  HttpCode,
  Patch,
  Post,
} from '@nestjs/common';
import { Throttle } from '@nestjs/throttler';
import { PASSWORD_THROTTLE } from '../../../config/throttle.config';
import {
  ApiBearerAuth,
  ApiOperation,
  ApiResponse,
  ApiTags,
} from '@nestjs/swagger';
import { CurrentUser } from '../../../common/decorators';
import { CurrentUserDto } from '../../../common/dtos/current-user.dto';
import { ErrorResponseDto } from '../../../common/dtos/error-response.dto';
import {
  ChangePasswordDto,
  ProfileResponseDto,
  UpdateProfileDto,
} from '../dtos';
import { ProfileService } from '../services/profile.service';

@Controller('auth')
@ApiTags('Profile')
@ApiBearerAuth()
export class ProfileController {
  constructor(private readonly profileService: ProfileService) {}

  @Get('profile')
  @Header('Cache-Control', 'no-store')
  @ApiOperation({
    summary: 'Your profile',
    description: 'Username, email and full name of the signed-in user',
  })
  @ApiResponse({ status: 200, type: ProfileResponseDto })
  async getProfile(
    @CurrentUser() user: CurrentUserDto,
  ): Promise<ProfileResponseDto> {
    return this.profileService.getProfile(user.userId);
  }

  @Patch('profile')
  @Header('Cache-Control', 'no-store')
  @ApiOperation({
    summary: 'Update your profile',
    description: 'Change your own full name and/or email',
  })
  @ApiResponse({ status: 200, type: ProfileResponseDto })
  @ApiResponse({
    status: 409,
    description: 'Email used by another account',
    type: ErrorResponseDto,
  })
  @ApiResponse({
    status: 422,
    description: 'Invalid field, or a field that cannot be changed here',
    type: ErrorResponseDto,
  })
  async updateProfile(
    @CurrentUser() user: CurrentUserDto,
    @Body() dto: UpdateProfileDto,
  ): Promise<ProfileResponseDto> {
    return this.profileService.updateProfile(user.userId, dto);
  }

  @Post('password')
  @HttpCode(204)
  @Throttle(PASSWORD_THROTTLE)
  @ApiOperation({
    summary: 'Change your password',
    description:
      'Checks your current password, saves the new one and signs out your other sessions',
  })
  @ApiResponse({ status: 204, description: 'Password changed' })
  @ApiResponse({
    status: 403,
    description: 'Current password is incorrect',
    type: ErrorResponseDto,
  })
  @ApiResponse({
    status: 409,
    description: 'Password changed somewhere else at the same time',
    type: ErrorResponseDto,
  })
  @ApiResponse({
    status: 422,
    description: 'New password too short, too long, blank or unchanged',
    type: ErrorResponseDto,
  })
  @ApiResponse({
    status: 429,
    description: 'Too many attempts',
    type: ErrorResponseDto,
  })
  async changePassword(
    @CurrentUser() user: CurrentUserDto,
    @Body() dto: ChangePasswordDto,
  ): Promise<void> {
    await this.profileService.changePassword(user.userId, dto);
  }
}
