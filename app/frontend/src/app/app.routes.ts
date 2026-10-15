import { Routes } from '@angular/router';
import { authGuard, guestGuard } from './services/auth';

export const routes: Routes = [
  {
    path: 'login',
    loadComponent: () => import('./features/auth/login/login').then((m) => m.Login),
    canActivate: [guestGuard],
  },
  {
    path: 'register',
    loadComponent: () => import('./features/auth/register/register').then((m) => m.Register),
    canActivate: [guestGuard],
  },
  {
    path: '',
    canActivateChild: [authGuard],
    children: [
      {
        path: 'dashboard',
        loadComponent: () =>
          import('./features/dashboard/dashboard/dashboard').then((m) => m.Dashboard),
      },
      {
        path: 'orders',
        loadComponent: () =>
          import('./features/orders/orders-list/orders-list').then((m) => m.OrdersList),
      },
      {
        path: 'instruments',
        loadComponent: () =>
          import('./features/instruments/instruments-list/instruments-list').then(
            (m) => m.InstrumentsList,
          ),
      },
      {
        path: 'instruments/:id',
        loadComponent: () =>
          import('./features/instruments/instruments-detail/instruments-detail').then(
            (m) => m.InstrumentsDetail,
          ),
      },
      {
        path: 'positions',
        loadComponent: () =>
          import('./features/positions/positions-list/positions-list').then((m) => m.PositionsList),
      },
      {
        path: 'positions/:id',
        loadComponent: () =>
          import('./features/positions/positions-detail/positions-detail').then(
            (m) => m.PositionsDetail,
          ),
      },
      {
        path: 'wallet/return',
        loadComponent: () =>
          import('./features/wallet/wallet-return/wallet-return').then((m) => m.WalletReturn),
      },
      { path: '', redirectTo: 'dashboard', pathMatch: 'full' },
    ],
  },
  { path: '**', redirectTo: 'dashboard' },
];
