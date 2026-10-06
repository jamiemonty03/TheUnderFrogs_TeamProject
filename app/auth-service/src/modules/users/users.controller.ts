import { Controller, Get, Post, Body, Param, Put, Delete, HttpCode } from '@nestjs/common';
import { UsersService } from './users.service';
import { CreateUserDto, UpdateUserDto, UserResponseDto, toUserResponse } from './dto';

@Controller('users')
export class UsersController {
  constructor(private readonly usersService: UsersService) {}

  @Post()
  @HttpCode(201)
  async createUser(@Body() createUserDto: CreateUserDto): Promise<UserResponseDto> {
    return toUserResponse(await this.usersService.createUser(createUserDto));
  }

  @Get()
  async getAllUsers(): Promise<UserResponseDto[]> {
    return (await this.usersService.getAllUsers()).map(toUserResponse);
  }

  @Get(':id')
  async getUserById(@Param('id') id: string): Promise<UserResponseDto> {
    return toUserResponse(await this.usersService.getUserById(Number(id)));
  }

  @Put(':id')
  async updateUser(@Param('id') id: string, @Body() updateUserDto: UpdateUserDto): Promise<UserResponseDto> {
    return toUserResponse(await this.usersService.updateUser(Number(id), updateUserDto));
  }

  @Delete(':id')
  @HttpCode(204)
  async deleteUser(@Param('id') id: string) {
    await this.usersService.deleteUser(Number(id));
  }
}
