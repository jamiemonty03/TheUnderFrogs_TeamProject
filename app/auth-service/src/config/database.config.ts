import { TypeOrmModuleOptions } from '@nestjs/typeorm';
import { User } from '../modules/users/entities/user.entity';
import { Auth } from '../modules/auth/entities/auth.entity';
import { RefreshToken } from '../modules/tokens/entities/refresh-token.entity';
import { ServiceClient } from '../modules/clients/entities/service-client.entity';

export const databaseConfig = (): TypeOrmModuleOptions => ({
  type: 'postgres',
  host: process.env.DB_HOST!,
  port: parseInt(process.env.DB_PORT || '5432', 10),
  username: process.env.DB_USERNAME!,
  password: process.env.DB_PASSWORD!,
  database: process.env.DB_NAME!,
  entities: [User, Auth, RefreshToken, ServiceClient],
  synchronize: false,
  logging: process.env.NODE_ENV === 'development',
});
