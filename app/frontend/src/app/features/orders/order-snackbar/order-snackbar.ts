import { CommonModule } from '@angular/common';
import { Component, ViewEncapsulation, inject } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MAT_SNACK_BAR_DATA, MatSnackBarRef } from '@angular/material/snack-bar';

export interface OrderSnackbarData {
  orderId: string;
  status: 'FILLED' | 'REJECTED';
  reason?: string | null;
}

@Component({
  encapsulation: ViewEncapsulation.None,
  imports: [CommonModule, MatButtonModule],
  selector: 'app-order-snackbar',
  styleUrl: './order-snackbar.scss',
  templateUrl: './order-snackbar.html',
})
export class OrderSnackbar {
  readonly data = inject<OrderSnackbarData>(MAT_SNACK_BAR_DATA);
  private readonly snackBarRef = inject(MatSnackBarRef<OrderSnackbar>);

  dismiss(): void {
    this.snackBarRef.dismiss();
  }
}
