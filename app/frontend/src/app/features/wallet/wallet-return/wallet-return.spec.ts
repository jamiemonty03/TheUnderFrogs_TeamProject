import { HttpErrorResponse } from '@angular/common/http';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { MatSnackBar } from '@angular/material/snack-bar';
import { ActivatedRoute, Router, convertToParamMap, provideRouter } from '@angular/router';
import { Observable, Subject, of, throwError } from 'rxjs';
import { vi } from 'vitest';
import { TopUpConfirmation, Wallet } from '../../../services/wallet';
import { WALLET_ERRORS } from '../wallet-error';
import { CANCELLED_NOTICE, WalletReturn, safeWalletReturn } from './wallet-return';

describe('WalletReturn', () => {
  let fixture: ComponentFixture<WalletReturn>;
  let page: HTMLElement;
  let confirmTopUp: ReturnType<typeof vi.fn<(sessionId: string) => Observable<TopUpConfirmation>>>;
  let navigateByUrl: ReturnType<typeof vi.spyOn>;
  let snackBarOpen: ReturnType<typeof vi.fn>;

  async function setUp(
    params: Record<string, string>,
    result: Observable<TopUpConfirmation> = of({ amount: 250, cashBalance: 750 }),
  ) {
    confirmTopUp = vi.fn(() => result);
    snackBarOpen = vi.fn();
    await TestBed.configureTestingModule({
      imports: [WalletReturn],
      providers: [
        provideRouter([]),
        { provide: Wallet, useValue: { confirmTopUp } },
        { provide: MatSnackBar, useValue: { open: snackBarOpen } },
        {
          provide: ActivatedRoute,
          useValue: { snapshot: { queryParamMap: convertToParamMap(params) } },
        },
      ],
    }).compileComponents();
    navigateByUrl = vi.spyOn(TestBed.inject(Router), 'navigateByUrl').mockResolvedValue(true);
    fixture = TestBed.createComponent(WalletReturn);
    page = fixture.nativeElement;
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();
  }

  const byTestId = (id: string) => page.querySelector<HTMLElement>(`[data-testid="${id}"]`);

  describe('after paying', () => {
    it('confirms the payment, returns to the page the user came from and says how much was added', async () => {
      await setUp({ session_id: 'cs_test_1', returnTo: '/positions' });

      expect(confirmTopUp).toHaveBeenCalledWith('cs_test_1');
      expect(navigateByUrl).toHaveBeenCalledWith('/positions', { replaceUrl: true });
      expect(snackBarOpen).toHaveBeenCalledWith('Added $250.00 to your cash balance.', 'OK', {
        duration: 6000,
      });
    });

    it('formats larger amounts with thousands separators', async () => {
      await setUp({ session_id: 'cs_test_1' }, of({ amount: 10000, cashBalance: 10500 }));

      expect(snackBarOpen).toHaveBeenCalledWith(
        'Added $10,000.00 to your cash balance.',
        'OK',
        expect.anything(),
      );
    });

    it('shows "Confirming your payment…" while waiting', async () => {
      await setUp({ session_id: 'cs_test_1' }, new Subject<TopUpConfirmation>());

      expect(byTestId('wallet-return-confirming')?.textContent).toContain(
        'Confirming your payment…',
      );
      expect(byTestId('wallet-return-confirming')?.getAttribute('role')).toBe('status');
      expect(navigateByUrl).not.toHaveBeenCalled();
    });

    it('goes to the dashboard when there is no returnTo', async () => {
      await setUp({ session_id: 'cs_test_1' });

      expect(navigateByUrl).toHaveBeenCalledWith('/dashboard', { replaceUrl: true });
    });
  });

  describe('when the payment cannot be added', () => {
    const httpError = (status: number, errorCode?: string, message = 'from server') =>
      new HttpErrorResponse({ status, error: errorCode ? { errorCode, message } : null });

    it.each([
      [
        'not completed (PAY-409)',
        httpError(409, 'PAY-409', "This payment hasn't been completed"),
        "This payment hasn't been completed.",
      ],
      [
        'another account (AUTH-403)',
        httpError(403, 'AUTH-403', 'This payment belongs to a different account'),
        'This payment belongs to a different account.',
      ],
      ['unknown (PAY-404)', httpError(404, 'PAY-404'), "We couldn't find that payment."],
    ])('explains a payment that is %s, without a retry button', async (_label, error, message) => {
      await setUp(
        { session_id: 'cs_test_1', returnTo: '/orders' },
        throwError(() => error),
      );

      expect(byTestId('wallet-return-error')?.textContent?.trim()).toBe(message);
      expect(byTestId('wallet-return-error')?.getAttribute('role')).toBe('alert');
      expect(byTestId('wallet-return-retry')).toBeNull();
      expect(navigateByUrl).not.toHaveBeenCalled();
      expect(snackBarOpen).not.toHaveBeenCalled();
    });

    it('offers a retry after a temporary failure, and the retry can succeed', async () => {
      await setUp(
        { session_id: 'cs_test_1', returnTo: '/orders' },
        throwError(() => httpError(0)),
      );
      expect(byTestId('wallet-return-error')?.textContent?.trim()).toBe(WALLET_ERRORS.unreachable);

      confirmTopUp.mockReturnValue(of({ amount: 50, cashBalance: 550 }));
      byTestId('wallet-return-retry')!.click();
      await fixture.whenStable();

      expect(confirmTopUp).toHaveBeenCalledTimes(2);
      expect(navigateByUrl).toHaveBeenCalledWith('/orders', { replaceUrl: true });
      expect(snackBarOpen).toHaveBeenCalledWith(
        'Added $50.00 to your cash balance.',
        'OK',
        expect.anything(),
      );
    });

    it('"Go back" returns to the page the user came from', async () => {
      await setUp(
        { session_id: 'cs_test_1', returnTo: '/orders' },
        throwError(() => httpError(409, 'PAY-409', "This payment hasn't been completed")),
      );

      byTestId('wallet-return-back')!.click();

      expect(navigateByUrl).toHaveBeenCalledWith('/orders', { replaceUrl: true });
    });
  });

  describe('when the user cancels on Stripe', () => {
    it('returns to their page, says nothing was taken, and does not confirm anything', async () => {
      await setUp({ cancelled: '1', returnTo: '/positions' });

      expect(confirmTopUp).not.toHaveBeenCalled();
      expect(navigateByUrl).toHaveBeenCalledWith('/positions', { replaceUrl: true });
      expect(snackBarOpen).toHaveBeenCalledWith(CANCELLED_NOTICE, 'OK', { duration: 6000 });
    });
  });

  it('with nothing to confirm, quietly returns without confirming or showing a message', async () => {
    await setUp({ returnTo: '/positions' });

    expect(confirmTopUp).not.toHaveBeenCalled();
    expect(navigateByUrl).toHaveBeenCalledWith('/positions', { replaceUrl: true });
    expect(snackBarOpen).not.toHaveBeenCalled();
  });

  describe('safeWalletReturn', () => {
    it.each([
      ['/positions', '/positions'],
      ['/orders?status=NEW', '/orders?status=NEW'],
      [null, '/dashboard'],
      ['https://evil.example.com', '/dashboard'],
      ['//evil.example.com', '/dashboard'],
      ['/\\evil.example.com', '/dashboard'],
      ['/login', '/dashboard'],
      ['/wallet/return?session_id=cs_test_1', '/dashboard'],
    ])('turns %s into %s', (input, expected) => {
      expect(safeWalletReturn(input)).toBe(expected);
    });
  });
});
