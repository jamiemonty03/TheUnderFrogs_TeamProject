import { NestFactory } from '@nestjs/core';
import { ConfigService } from '@nestjs/config';
import { OpenAPIObject, SwaggerModule } from '@nestjs/swagger';
import { readFileSync } from 'fs';
import { join } from 'path';
import { load } from 'js-yaml';
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
  console.log(`Swagger docs available at http://localhost:${port}/docs`);
}
void bootstrap();
