import { Body, Controller, Get, Header, Patch } from '@nestjs/common';
import { ApiBearerAuth, ApiOperation, ApiResponse, ApiTags } from '@nestjs/swagger';
import { CurrentUser } from '../../../common/decorators';
import { CurrentUserDto } from '../../../common/dtos/current-user.dto';
import { ErrorResponseDto } from '../../../common/dtos/error-response.dto';
import { ProfileResponseDto, UpdateProfileDto } from '../dtos';
import { ProfileService } from '../services/profile.service';

@Controller('auth/profile')
@ApiTags('Profile')
@ApiBearerAuth()
export class ProfileController {
  constructor(private readonly profileService: ProfileService) {}

  @Get()
  @Header('Cache-Control', 'no-store')
  @ApiOperation({ summary: 'Your profile', description: 'Username, email and full name of the signed-in user' })
  @ApiResponse({ status: 200, type: ProfileResponseDto })
  async getProfile(@CurrentUser() user: CurrentUserDto): Promise<ProfileResponseDto> {
    return this.profileService.getProfile(user.userId);
  }

  @Patch()
  @Header('Cache-Control', 'no-store')
  @ApiOperation({ summary: 'Update your profile', description: 'Change your own full name and/or email' })
  @ApiResponse({ status: 200, type: ProfileResponseDto })
  @ApiResponse({ status: 409, description: 'Email used by another account', type: ErrorResponseDto })
  @ApiResponse({ status: 422, description: 'Invalid field, or a field that cannot be changed here', type: ErrorResponseDto })
  async updateProfile(
    @CurrentUser() user: CurrentUserDto,
    @Body() dto: UpdateProfileDto,
  ): Promise<ProfileResponseDto> {
    return this.profileService.updateProfile(user.userId, dto);
  }
}
