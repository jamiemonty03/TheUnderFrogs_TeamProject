import * as Joi from 'joi';
import { parsePrivateKey } from './signing-key';

export const validationSchema = Joi.object({
  NODE_ENV: Joi.string().valid('development', 'production', 'test').default('development'),
  APP_PORT: Joi.number().default(8081),
  DB_HOST: Joi.string().required(),
  DB_PORT: Joi.number().default(5432),
  DB_USERNAME: Joi.string().required(),
  DB_PASSWORD: Joi.string().required(),
  DB_NAME: Joi.string().required(),
  JWT_SECRET: Joi.string().required().min(32),
  JWT_PRIVATE_KEY: Joi.string()
    .base64()
    .required()
    .custom((value: string) => {
      parsePrivateKey(value);
      return value;
    }, 'RSA private key'),
  JWT_KEY_ID: Joi.string().pattern(/^[A-Za-z0-9._-]{1,64}$/).required(),
  JWT_ISSUER: Joi.string().default('auth-service'),
  JWT_AUDIENCE: Joi.string().default('trading-platform'),
  JWT_ACCESS_TOKEN_TTL: Joi.number().integer().min(60).max(3600).default(900),
});
