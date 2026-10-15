import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { catchError, map, of } from 'rxjs';
import { Auth } from './auth';

const HOME_URL = '/dashboard';

export const authGuard: CanActivateFn = (_route, state) => {
  const auth = inject(Auth);
  const router = inject(Router);

  const toLogin = (expired: boolean) =>
    router.createUrlTree(['/login'], {
      queryParams: {
        ...(state.url === HOME_URL ? {} : { returnUrl: state.url }),
        ...(expired ? { expired: 1 } : {}),
      },
    });

  if (auth.isAuthenticated()) {
    return true;
  }
  if (!auth.canRefresh()) {
    auth.clearSession();
    return toLogin(false);
  }
  return auth.refresh().pipe(
    map(() => true),
    catchError(() => of(toLogin(true))),
  );
};
