import { Routes } from '@angular/router';
import { Login } from './features/auth/login/login';
import { Register } from './features/auth/register/register';
import { Dashboard } from './features/dashboard/dashboard/dashboard';
import { OrdersList } from './features/orders/orders-list/orders-list';
import { InstrumentsList } from './features/instruments/instruments-list/instruments-list';
import { InstrumentsDetail } from './features/instruments/instruments-detail/instruments-detail';
import { PositionsDetail } from './features/positions/positions-detail/positions-detail';

export const routes: Routes = [
  { path: 'login', component: Login },
  { path: 'register', component: Register },
  { path: 'dashboard', component: Dashboard },
  { path: 'orders', component: OrdersList },
  { path: 'instruments', component: InstrumentsList },
  { path: 'instruments/:id', component: InstrumentsDetail },
  {
    path: 'positions',
    loadComponent: () => import('./features/positions/positions-list/positions-list').then((m) => m.PositionsList),
  },
  { path: 'positions/:id', component: PositionsDetail },
  { path: '', redirectTo: '/dashboard', pathMatch: 'full' },
  { path: '**', redirectTo: '/dashboard' },
];
