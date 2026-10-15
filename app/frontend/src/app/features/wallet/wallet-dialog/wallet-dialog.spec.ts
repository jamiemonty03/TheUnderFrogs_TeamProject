import { HttpErrorResponse } from '@angular/common/http';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { signal } from '@angular/core';
import { MatDialogRef } from '@angular/material/dialog';
import { Router, provideRouter } from '@angular/router';
import { Observable, Subject, of, throwError } from 'rxjs';
import { vi } from 'vitest';
import { Accounts } from '../../../services/accounts';
import { ExternalNavigation } from '../../../services/external-navigation';
import { Wallet } from '../../../services/wallet';
import { WALLET_ERRORS } from '../wallet-error';
import { WalletDialog } from './wallet-dialog';

const CHECKOUT_URL = 'https://checkout.stripe.com/c/pay/cs_test_1';

describe('WalletDialog', () => {
  let fixture: ComponentFixture<WalletDialog>;
  let page: HTMLElement;
  let startTopUp: ReturnType<
    typeof vi.fn<(amount: number, returnTo: string) => Observable<string>>
  >;
  let goTo: ReturnType<typeof vi.fn>;
  let close: ReturnType<typeof vi.fn>;
  let getBalance: ReturnType<typeof vi.fn>;

  async function setUp(balance$: Observable<number> = of(97949.15)) {
    startTopUp = vi.fn(() => of(CHECKOUT_URL));
    goTo = vi.fn();
    close = vi.fn();
    getBalance = vi.fn(() => balance$);
    await TestBed.configureTestingModule({
      imports: [WalletDialog],
      providers: [
        provideRouter([]),
        { provide: Accounts, useValue: { getBalance, balance: { loading: signal(false) } } },
        { provide: Wallet, useValue: { startTopUp } },
        { provide: ExternalNavigation, useValue: { goTo } },
        { provide: MatDialogRef, useValue: { close } },
      ],
    }).compileComponents();
    vi.spyOn(TestBed.inject(Router), 'url', 'get').mockReturnValue('/positions');
    fixture = TestBed.createComponent(WalletDialog);
    page = fixture.nativeElement;
    fixture.detectChanges();
  }

  const byTestId = <T extends HTMLElement = HTMLElement>(id: string) =>
    page.querySelector<T>(`[data-testid="${id}"]`);
  const amountInput = () => byTestId<HTMLInputElement>('wallet-amount')!;
  const submitButton = () => byTestId<HTMLButtonElement>('wallet-submit')!;
  const errors = () =>
    Array.from(page.querySelectorAll('mat-error')).map((e) => e.textContent?.trim());
  const typeAmount = (value: string) => {
    amountInput().value = value;
    amountInput().dispatchEvent(new Event('input'));
    fixture.detectChanges();
  };
  const submit = async () => {
    submitButton().click();
    fixture.detectChanges();
    await fixture.whenStable();
  };

  describe('balance', () => {
    it('shows the current cash balance', async () => {
      await setUp();

      expect(getBalance).toHaveBeenCalledTimes(1);
      expect(byTestId('wallet-balance')?.textContent?.trim()).toBe('$97,949.15');
    });

    it('shows a dash when the balance cannot be loaded', async () => {
      await setUp(throwError(() => new Error('down')));

      expect(byTestId('wallet-balance')?.textContent?.trim()).toBe('—');
    });
  });

  describe('choosing an amount', () => {
    beforeEach(() => setUp());

    it.each([
      [100, '$100'],
      [500, '$500'],
      [1000, '$1,000'],
    ])('the $%s quick pick fills in the amount', (amount, label) => {
      const quick = byTestId<HTMLButtonElement>(`wallet-quick-${amount}`)!;
      expect(quick.textContent?.trim()).toBe(label);

      quick.click();
      fixture.detectChanges();

      expect(amountInput().value).toBe(String(amount));
      expect(quick.getAttribute('aria-pressed')).toBe('true');
    });

    it.each([
      ['', 'Enter an amount.'],
      ['9.99', 'The minimum top-up is $10.00.'],
      ['10000.01', 'The maximum top-up is $10,000.00.'],
      ['25.555', 'Use at most 2 decimal places.'],
    ])('rejects "%s" with "%s" and does not start a payment', async (value, message) => {
      typeAmount(value);

      await submit();

      expect(errors()).toEqual([message]);
      expect(startTopUp).not.toHaveBeenCalled();
    });

    it.each(['10', '10000', '250.5'])('accepts %s', async (value) => {
      typeAmount(value);

      await submit();

      expect(errors()).toEqual([]);
      expect(startTopUp).toHaveBeenCalledWith(Number(value), '/positions');
    });
  });

  describe('paying', () => {
    it('starts the top-up with the current page as returnTo, then goes to Stripe', async () => {
      await setUp();
      typeAmount('250');

      await submit();

      expect(startTopUp).toHaveBeenCalledWith(250, '/positions');
      expect(goTo).toHaveBeenCalledWith(CHECKOUT_URL);
    });

    it('shows "Opening secure payment…" and ignores a second click while waiting', async () => {
      await setUp();
      startTopUp.mockReturnValue(new Subject<string>());
      typeAmount('250');

      await submit();
      await submit();

      expect(submitButton().disabled).toBe(true);
      expect(submitButton().textContent?.trim()).toBe('Opening secure payment…');
      expect(startTopUp).toHaveBeenCalledTimes(1);
    });

    it('says adding funds is unavailable and disables the button when Stripe is not set up', async () => {
      await setUp();
      startTopUp.mockReturnValue(
        throwError(
          () =>
            new HttpErrorResponse({
              status: 503,
              error: { errorCode: 'PAY-503', message: "Adding funds isn't available right now" },
            }),
        ),
      );
      typeAmount('250');

      await submit();

      expect(byTestId('wallet-error')?.textContent?.trim()).toBe(WALLET_ERRORS.unavailable);
      expect(submitButton().disabled).toBe(true);
      expect(goTo).not.toHaveBeenCalled();
    });

    it('shows the error and lets the user try again after a temporary failure', async () => {
      await setUp();
      startTopUp.mockReturnValue(throwError(() => new HttpErrorResponse({ status: 0 })));
      typeAmount('250');

      await submit();

      expect(byTestId('wallet-error')?.textContent?.trim()).toBe(WALLET_ERRORS.unreachable);
      expect(submitButton().disabled).toBe(false);
      expect(byTestId('wallet-error')?.getAttribute('role')).toBe('alert');
    });
  });

  it('closes when the close button is clicked', async () => {
    await setUp();

    byTestId<HTMLButtonElement>('wallet-close')!.click();

    expect(close).toHaveBeenCalled();
  });
});
