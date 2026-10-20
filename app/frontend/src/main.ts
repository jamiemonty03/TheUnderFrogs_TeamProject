import { bootstrapApplication } from '@angular/platform-browser';
import { appConfig } from './app/app.config';
import { App } from './app/app';
import { applyInitialTheme } from './app/services/theme';

applyInitialTheme();

bootstrapApplication(App, appConfig)
  .catch((err) => console.error(err));
