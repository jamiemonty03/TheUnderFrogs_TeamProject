// Run in the auth-service image by the register-client task. Does what the database half of
// app/auth-service/scripts/register-service-client.sh does: hashes the client secret with the
// service's own PasswordHasher and upserts it into service_clients.
require('reflect-metadata');
const { Client } = require('pg');
const { PasswordHasher } = require('./dist/modules/users/services/password-hasher.service.js');

(async () => {
  const hash = await new PasswordHasher().hash(process.env.AUTH_CLIENT_SECRET);
  const db = new Client({
    host: process.env.DB_HOST,
    port: 5432,
    user: process.env.DB_USERNAME,
    password: process.env.DB_PASSWORD,
    database: process.env.DB_NAME,
    ssl: process.env.DB_SSL === 'true' ? { rejectUnauthorized: false } : false,
  });
  await db.connect();
  await db.query(
    `INSERT INTO service_clients (client_id, client_secret_hash)
     VALUES ($1, $2)
     ON CONFLICT (client_id) DO UPDATE
     SET client_secret_hash = EXCLUDED.client_secret_hash, is_active = true, updated_at = NOW()`,
    [process.env.AUTH_CLIENT_ID, hash],
  );
  await db.end();
  console.log(`Registered service client ${process.env.AUTH_CLIENT_ID} in ${process.env.DB_NAME}`);
})().catch((err) => {
  console.error(err);
  process.exit(1);
});
