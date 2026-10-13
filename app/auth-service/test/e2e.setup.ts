import { generateKeyPairSync } from 'crypto';

import '@nestjs/throttler';

process.env.NODE_ENV = 'test';
process.env.DB_HOST ??= 'localhost';
process.env.DB_PORT ??= '5438';

const pem = generateKeyPairSync('rsa', { modulusLength: 2048 }).privateKey.export({ type: 'pkcs8', format: 'pem' });
process.env.JWT_PRIVATE_KEY = Buffer.from(pem.toString()).toString('base64');
process.env.JWT_KEY_ID = 'e2e-key';
