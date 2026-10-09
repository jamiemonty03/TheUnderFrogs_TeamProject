import { Injectable, ConflictException, Logger, NotFoundException } from '@nestjs/common';
import { UsersRepository } from './users.repository';
import { PasswordHasher } from './password-hasher.service';
import { CreateUserDto, DEFAULT_ROLE, UpdateUserDto } from './dto';
import { User } from './entities/user.entity';

@Injectable()
export class UsersService {
  private readonly logger = new Logger(UsersService.name);
  private dummyHash?: Promise<string>;

  constructor(
    private readonly usersRepository: UsersRepository,
    private readonly passwordHasher: PasswordHasher,
  ) {}

  async createUser(createUserDto: CreateUserDto): Promise<User> {
    const { username, email, password, role, full_name } = createUserDto;

    // Check if user already exists
    const existingUser = await this.usersRepository.findByUsername(username);
    if (existingUser) {
      throw new ConflictException(`User with username ${username} already exists`);
    }

    const existingEmail = await this.usersRepository.findByEmail(email);
    if (existingEmail) {
      throw new ConflictException(`User with email ${email} already exists`);
    }

    const hashedPassword = await this.passwordHasher.hash(password);

    return this.usersRepository.create({
      username,
      email,
      password_hash: hashedPassword,
      full_name: full_name ?? null,
      roles: [(role || DEFAULT_ROLE).toUpperCase()],
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
      changes.password_hash = await this.passwordHasher.hash(password);
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

  async linkAccount(id: number, accountId: string): Promise<User> {
    const updated = await this.usersRepository.update(id, { account_id: accountId });
    if (!updated) {
      throw new NotFoundException(`User with id ${id} not found`);
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

  async verifyCredentials(username: string, password: string): Promise<User | null> {
    const user = await this.usersRepository.findByUsername(username);
    if (!user) {
      await this.passwordHasher.verify(password, await this.getDummyHash());
      return null;
    }

    if (!(await this.passwordHasher.verify(password, user.password_hash))) {
      return null;
    }

    if (this.passwordHasher.needsRehash(user.password_hash)) {
      await this.rehash(user, password);
    }
    return user;
  }

  private async rehash(user: User, password: string): Promise<void> {
    try {
      const newHash = await this.passwordHasher.hash(password);
      if (await this.usersRepository.updatePasswordHash(user.id, user.password_hash, newHash)) {
        user.password_hash = newHash;
      }
    } catch (error) {
      this.logger.warn(`Password rehash failed for user ${user.id}: ${(error as Error).message}`);
    }
  }

  private getDummyHash(): Promise<string> {
    this.dummyHash ??= this.passwordHasher.hash('dummy-password-for-timing');
    return this.dummyHash;
  }
}
