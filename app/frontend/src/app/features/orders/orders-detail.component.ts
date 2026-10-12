import { Component } from '@angular/core';
import { RouterLink } from '@angular/router';

@Component({
  selector: 'app-orders-detail',
  standalone: true,
  imports: [RouterLink],
  template: `
    <div>
      <h2>Create New Order</h2>
      <p>Order creation form placeholder</p>
      <a routerLink="/orders">Back to Orders</a>
    </div>
  `,
  styles: [],
})
export class OrdersDetailComponent {}
