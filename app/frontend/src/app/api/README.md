# Generated API clients

Everything in this folder except this README is generated from the services' OpenAPI specs by
`npm run generate:api` (see `api-clients.json` and `scripts/generate-api.mjs`).

- Do not edit these files. Change the service's `docs/*.yaml` and regenerate instead.
- CI regenerates the clients and fails if they differ from what is committed.
- Base URLs come from `src/environments/environment*.ts` through `provideApiClients()` in `src/app/api-clients.ts`.
