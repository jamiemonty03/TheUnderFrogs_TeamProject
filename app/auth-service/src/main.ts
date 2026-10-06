import { NestFactory } from '@nestjs/core';
import { HttpStatus, ValidationPipe } from '@nestjs/common';
import { ConfigService } from '@nestjs/config';
import { OpenAPIObject, SwaggerModule } from '@nestjs/swagger';
import { readFileSync } from 'fs';
import { join } from 'path';
import { load } from 'js-yaml';
import { AppModule } from './app.module';
import { HttpExceptionFilter } from './common/filters/http-exception.filter';

async function bootstrap() {
  const app = await NestFactory.create(AppModule);

  app.useGlobalPipes(new ValidationPipe({
    whitelist: true,
    forbidNonWhitelisted: true,
    transform: true,
    errorHttpStatusCode: HttpStatus.UNPROCESSABLE_ENTITY,
  }));
  app.useGlobalFilters(new HttpExceptionFilter());

  // Serve the committed spec as-is so /docs always matches docs/auth-service.yaml
  const specPath = join(__dirname, '..', 'docs', 'auth-service.yaml');
  const document = load(readFileSync(specPath, 'utf8')) as OpenAPIObject;
  SwaggerModule.setup('docs', app, document, {
    swaggerOptions: {
      persistAuthorization: true,
      displayRequestDuration: true,
    },
  });

  const configService = app.get(ConfigService);
  const port = configService.get<number>('APP_PORT', 8081);

  await app.listen(port);
  console.log(`Auth service is running on port ${port}`);
  console.log(`Swagger docs available at http://localhost:${port}/docs`);
}
void bootstrap();
