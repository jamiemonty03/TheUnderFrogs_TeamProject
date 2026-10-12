import { Component } from '@angular/core';
import { RouterLink } from '@angular/router';

@Component({
  selector: 'app-orders',
  standalone: true,
  imports: [RouterLink],
  template: `
    <div>
      <h2>Orders</h2>
      <p>Orders list placeholder</p>
      <a routerLink="/orders/new">Create New Order</a>
    </div>
  `,
  styles: [],
})
export class OrdersComponent {}
