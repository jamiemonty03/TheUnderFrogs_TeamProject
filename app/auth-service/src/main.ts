import { NestFactory } from '@nestjs/core';
import { ConfigService } from '@nestjs/config';
import { OpenAPIObject, SwaggerModule } from '@nestjs/swagger';
import helmet from 'helmet';
import { NextFunction, Request, Response } from 'express';
import { readFileSync } from 'fs';
import { join } from 'path';
import { load } from 'js-yaml';
import { AppModule } from './app.module';
import { createValidationPipe } from './common/validation/validation.pipe';
import { JsonBodyAdapter } from './common/validation/json-body.adapter';
import { HttpExceptionFilter } from './common/filters/http-exception.filter';

async function bootstrap() {
  const app = await NestFactory.create(AppModule, new JsonBodyAdapter());
  const httpsEnabled = process.env.HTTPS_ENABLED === 'true';
  app.use(
    helmet({
      contentSecurityPolicy: { directives: { upgradeInsecureRequests: httpsEnabled ? [] : null } },
      hsts: httpsEnabled,
    }),
  );

  app.enableCors({
    origin: process.env.CORS_ORIGIN?.split(',') || ['http://localhost:4200'],
    credentials: true,
    methods: ['GET', 'POST', 'OPTIONS'],
    allowedHeaders: ['Content-Type', 'Authorization'],
  });

  app.use((
    req: Request,
    res: Response,
    next: NextFunction,
  ) => {
    if (req.headers['content-length'] && parseInt(req.headers['content-length']) > 1048576) {
      return res.status(413).json({ statusCode: 413, message: 'Payload too large' });
    }
    next();
  });

  app.useGlobalPipes(createValidationPipe());
  app.useGlobalFilters(new HttpExceptionFilter());

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
