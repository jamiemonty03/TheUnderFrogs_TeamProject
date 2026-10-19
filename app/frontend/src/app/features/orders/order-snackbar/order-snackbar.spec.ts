import { ComponentFixture, TestBed } from '@angular/core/testing';
import { MAT_SNACK_BAR_DATA, MatSnackBarRef } from '@angular/material/snack-bar';
import { OrderSnackbar, OrderSnackbarData } from './order-snackbar';

describe('OrderSnackbar', () => {
  let fixture: ComponentFixture<OrderSnackbar>;
  let snackBarRef: { dismiss: ReturnType<typeof vi.fn> };

  function createSnackbar(data: OrderSnackbarData): HTMLElement {
    snackBarRef = { dismiss: vi.fn() };

    TestBed.configureTestingModule({
      imports: [OrderSnackbar],
      providers: [
        { provide: MAT_SNACK_BAR_DATA, useValue: data },
        { provide: MatSnackBarRef, useValue: snackBarRef },
      ],
    });

    fixture = TestBed.createComponent(OrderSnackbar);
    fixture.detectChanges();
    return fixture.nativeElement as HTMLElement;
  }

  afterEach(() => TestBed.resetTestingModule());

  it('renders a filled order with success styling', () => {
    const page = createSnackbar({ orderId: 'ORDER-1', status: 'FILLED' });

    expect(page.querySelector('.order-snackbar')?.classList).toContain(
      'order-snackbar--success',
    );
    expect(page.querySelector('.order-snackbar__message')?.textContent).toContain(
      'Order ORDER-1 filled',
    );
    expect(page.querySelector('[role="status"]')).not.toBeNull();
  });

  it('renders a rejected order with its reason and error styling', () => {
    const page = createSnackbar({
      orderId: 'ORDER-2',
      status: 'REJECTED',
      reason: 'Insufficient funds',
    });

    expect(page.querySelector('.order-snackbar')?.classList).toContain(
      'order-snackbar--error',
    );
    expect(page.querySelector('.order-snackbar__message')?.textContent).toContain(
      'Order ORDER-2 rejected',
    );
    expect(page.querySelector('.order-snackbar__message')?.textContent).toContain(
      'Insufficient funds',
    );
  });

  it('renders a resting order with the price it is waiting for', () => {
    const page = createSnackbar({ orderId: 'ORDER-3', status: 'NEW', priceLimit: 350 });

    expect(page.querySelector('.order-snackbar__message')?.textContent?.replace(/\s+/g, ' ')).toContain(
      'Order ORDER-3 placed, waiting for $350.00',
    );
    expect(page.querySelector('.order-snackbar--success, .order-snackbar--error')).toBeNull();
  });

  it('renders a cancelled order', () => {
    const page = createSnackbar({ orderId: 'ORDER-4', status: 'CANCELLED' });

    expect(page.querySelector('.order-snackbar__message')?.textContent).toContain('Order ORDER-4 cancelled');
  });

  it('dismisses when the Dismiss button is clicked', () => {
    const page = createSnackbar({ orderId: 'ORDER-3', status: 'FILLED' });

    page.querySelector<HTMLButtonElement>('button')?.click();

    expect(snackBarRef.dismiss).toHaveBeenCalledOnce();
  });
});
