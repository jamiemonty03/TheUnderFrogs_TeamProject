import { Module } from '@nestjs/common';
import { TypeOrmModule } from '@nestjs/typeorm';
import { TokensService } from './tokens.service';
import { TokensRepository } from './tokens.repository';
import { Token } from './entities/token.entity';

@Module({
  imports: [TypeOrmModule.forFeature([Token])],
  providers: [TokensService, TokensRepository],
  exports: [TokensService, TokensRepository],
})
export class TokensModule {}
