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
  // RDS only accepts encrypted connections; the docker-compose Postgres has no SSL, so it stays off there.
  ssl: process.env.DB_SSL === 'true' ? { rejectUnauthorized: false } : false,
  entities: [User, Auth, RefreshToken, ServiceClient],
  synchronize: false,
  logging: process.env.NODE_ENV === 'development',
});
