# Frontend

This project was generated using [Angular CLI](https://github.com/angular/angular-cli) version 22.2.2.

## Development server

To start a local development server, run:

```bash
ng serve
```

Once the server is running, open your browser and navigate to `http://localhost:4200/`. The application will automatically reload whenever you modify any of the source files.

## Code scaffolding

Angular CLI includes powerful code scaffolding tools. To generate a new component, run:

```bash
ng generate component component-name
```

For a complete list of available schematics (such as `components`, `directives`, or `pipes`), run:

```bash
ng generate --help
```

## Building

To build the project run:

```bash
ng build
```

This will compile your project and store the build artifacts in the `dist/` directory. By default, the production build optimizes your application for performance and speed.

## Running unit tests

To execute unit tests with the [Vitest](https://vitest.dev/) test runner, use the following command:

```bash
ng test
```

## Running end-to-end tests

The e2e tests use [Playwright](https://playwright.dev/) and live in `e2e/`. They run against the real app, so start the stack first, from the repo root:

```bash
docker-compose up -d --build
```

Then run them from the repo root, inside Playwright's Docker image (its browsers don't install on Amazon Linux).

Run them all once:

```bash
docker run --rm --network host --ipc=host -v "$PWD/app/frontend":/app -v /app/node_modules -w /app \
  mcr.microsoft.com/playwright:v1.64.0-noble \
  sh -c "npm ci && npx playwright test; s=\$?; chown -R $(id -u):$(id -g) test-results playwright-report playwright 2>/dev/null; exit \$s"
```

Or open the interactive UI, then open port 9323 in your browser and click ▶ to run tests and watch each step:

```bash
docker run --rm -it --network host --ipc=host -v "$PWD/app/frontend":/app -v /app/node_modules -w /app \
  mcr.microsoft.com/playwright:v1.64.0-noble \
  sh -c "npm ci && npx playwright test --ui-host=0.0.0.0 --ui-port=9323"
```

The auth-service allows 5 logins a minute, so if a test fails with "Too many attempts", wait a minute and run again.

## Additional Resources

For more information on using the Angular CLI, including detailed command references, visit the [Angular CLI Overview and Command Reference](https://angular.dev/tools/cli) page.
