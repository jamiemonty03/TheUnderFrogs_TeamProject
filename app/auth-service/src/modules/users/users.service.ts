import { Injectable, ConflictException, NotFoundException } from '@nestjs/common';
import * as bcrypt from 'bcryptjs';
import { UsersRepository } from './users.repository';
import { CreateUserDto, UpdateUserDto } from './dto';
import { User } from './entities/user.entity';

@Injectable()
export class UsersService {
  constructor(private readonly usersRepository: UsersRepository) {}

  async createUser(createUserDto: CreateUserDto): Promise<User> {
    const { username, email, password, role } = createUserDto;

    // Check if user already exists
    const existingUser = await this.usersRepository.findByUsername(username);
    if (existingUser) {
      throw new ConflictException(`User with username ${username} already exists`);
    }

    const existingEmail = await this.usersRepository.findByEmail(email);
    if (existingEmail) {
      throw new ConflictException(`User with email ${email} already exists`);
    }

    // Hash password
    const hashedPassword = await bcrypt.hash(password, 10);

    return this.usersRepository.create({
      username,
      email,
      password_hash: hashedPassword,
      roles: [(role || 'USER').toUpperCase()],
    });
  }

  async getUserById(id: number): Promise<User> {
    const user = await this.usersRepository.findById(id);
    if (!user) {
      throw new NotFoundException(`User with id ${id} not found`);
    }
    return user;
  }

  async getUserByUsername(username: string): Promise<User> {
    const user = await this.usersRepository.findByUsername(username);
    if (!user) {
      throw new NotFoundException(`User with username ${username} not found`);
    }
    return user;
  }

  async getAllUsers(): Promise<User[]> {
    return this.usersRepository.findAll();
  }

  async updateUser(id: number, updateUserDto: UpdateUserDto): Promise<User> {
    await this.getUserById(id);

    const { password, role, ...rest } = updateUserDto;
    const changes: Partial<User> = { ...rest };
    if (password) {
      changes.password_hash = await bcrypt.hash(password, 10);
    }
    if (role) {
      changes.roles = [role.toUpperCase()];
    }

    const updated = await this.usersRepository.update(id, changes);
    if (!updated) {
      throw new NotFoundException(`Failed to update user with id ${id}`);
    }
    return updated;
  }

  async deleteUser(id: number): Promise<void> {
    await this.getUserById(id);
    const success = await this.usersRepository.delete(id);
    if (!success) {
      throw new NotFoundException(`Failed to delete user with id ${id}`);
    }
  }

  async validatePassword(plainPassword: string, hashedPassword: string): Promise<boolean> {
    return bcrypt.compare(plainPassword, hashedPassword);
  }
}
