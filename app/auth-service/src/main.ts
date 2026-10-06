import { NestFactory } from '@nestjs/core';
import { ConfigService } from '@nestjs/config';
import { AppModule } from './app.module';
import { createValidationPipe } from './common/validation/validation.pipe';
import { JsonBodyAdapter } from './common/validation/json-body.adapter';

async function bootstrap() {
  const app = await NestFactory.create(AppModule, new JsonBodyAdapter());

  app.useGlobalPipes(createValidationPipe());

  const configService = app.get(ConfigService);
  const port = configService.get<number>('APP_PORT', 8081);

  await app.listen(port);
  console.log(`Auth service is running on port ${port}`);
}
void bootstrap();
