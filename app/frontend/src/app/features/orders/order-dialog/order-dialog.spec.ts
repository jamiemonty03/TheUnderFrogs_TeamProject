import { HttpErrorResponse } from '@angular/common/http';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { signal } from '@angular/core';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { of, throwError } from 'rxjs';
import { vi } from 'vitest';
import { Accounts } from '../../../services/accounts';
import { Auth, CurrentUser } from '../../../services/auth/auth';
import { Instruments } from '../../../services/instruments';
import { TradingInstrument } from '../../../services/interfaces/instruments.interfaces';
import { OrderResponse } from '../../../services/interfaces/orders.interfaces';
import { Orders } from '../../../services/orders';
import { Portfolio } from '../../../services/interfaces/portfolios.interfaces';
import { Portfolios } from '../../../services/portfolios';
import { Position } from '../../../services/interfaces/positions.interfaces';
import { Positions } from '../../../services/positions';
import { OrderDialog, OrderDialogData } from './order-dialog';

const ACCOUNT = 'ACC0042';
const currentUser: CurrentUser = {
  userId: 42,
  username: 'trader',
  roles: ['TRADER'],
  accountId: ACCOUNT,
};
const AAPL: TradingInstrument = {
  symbol: 'AAPL',
  name: 'Apple Inc.',
  type: 'Stock',
  marketPrice: 100,
  lastUpdated: '2026-10-14T10:00:00Z',
};
const TSLA: TradingInstrument = {
  symbol: 'TSLA',
  name: 'Tesla Inc.',
  type: 'Stock',
  marketPrice: 250,
  lastUpdated: '2026-10-14T10:00:00Z',
};
const AAPL_POSITION: Position = {
  accountId: ACCOUNT,
  portfolioId: null,
  symbol: 'AAPL',
  quantity: 25,
  averageCost: 90,
};
const portfolio = (portfolioId: string, name: string, defaultPortfolio: boolean): Portfolio => ({
  portfolioId,
  accountId: ACCOUNT,
  name,
  risk: 'MEDIUM',
  benchmark: null,
  defaultPortfolio,
});
const MAIN = portfolio('PF0011', 'Main', true);
const GOLDEN_APPLE = portfolio('PF0013', 'Golden Apple', false);
const ORDER: OrderResponse = {
  orderId: 'ORDER-1',
  accountId: ACCOUNT,
  symbol: 'AAPL',
  side: 'BUY',
  quantity: 2,
  priceLimit: 100,
  orderStatus: 'NEW',
  version: 0,
  createdAt: '2026-10-14T10:00:00Z',
  lastUpdated: null,
  updatedBy: null,
};

function optionEvent(symbol: string): { option: { value: string } } {
  return { option: { value: symbol } };
}

describe('OrderDialog', () => {
  let fixture: ComponentFixture<OrderDialog>;
  let component: OrderDialog;
  let user: ReturnType<typeof signal<CurrentUser | null>>;
  let accounts: { getAccount: ReturnType<typeof vi.fn> };
  let instruments: { getAllTradableInstruments: ReturnType<typeof vi.fn> };
  let orders: { placeOrder: ReturnType<typeof vi.fn> };
  let positions: { getPosition: ReturnType<typeof vi.fn> };
  let dialogRef: { close: ReturnType<typeof vi.fn> };

  beforeEach(async () => {
    user = signal<CurrentUser | null>(currentUser);
    accounts = {
      getAccount: vi.fn(() =>
        of({ accountId: ACCOUNT, holderName: 'Trader', cashBalance: 1000, status: 'ACTIVE' }),
      ),
    };
    instruments = { getAllTradableInstruments: vi.fn(() => of([AAPL, TSLA])) };
    orders = { placeOrder: vi.fn(() => of(ORDER)) };
    positions = { getPosition: vi.fn(() => of(AAPL_POSITION)) };
    dialogRef = { close: vi.fn() };

    await TestBed.configureTestingModule({
      imports: [OrderDialog],
      providers: [
        { provide: Auth, useValue: { currentUser: user.asReadonly() } },
        { provide: Accounts, useValue: accounts },
        { provide: Instruments, useValue: instruments },
        { provide: Orders, useValue: orders },
        { provide: Positions, useValue: positions },
        { provide: Portfolios, useValue: { getPortfolios: vi.fn(() => of([MAIN, GOLDEN_APPLE])) } },
        { provide: MatDialogRef, useValue: dialogRef },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(OrderDialog);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('loads instruments and available cash on initialization', () => {
    expect(instruments.getAllTradableInstruments).toHaveBeenCalledTimes(1);
    expect(accounts.getAccount).toHaveBeenCalled();
    expect(component.instruments()).toEqual([AAPL, TSLA]);
    expect(component.availableCash()).toBe(1000);
  });

  it('stores selected tab and filters instrument matches', () => {
    component.setTab(1);
    component.buyInstrumentStep.controls.instrumentInput.setValue('tes');

    expect(component.selectedTab()).toBe(1);
    expect(component.buyMatches()).toEqual([TSLA]);
    expect(component.displayInstrument(AAPL)).toBe('AAPL - Apple Inc.');
    expect(component.displayInstrument('AAPL')).toBe('AAPL');
    expect(component.displayInstrument(null)).toBe('');
  });

  it('selects a BUY instrument and defaults its price limit to market price', () => {
    component.selectBuyInstrument(optionEvent('AAPL') as never);

    expect(component.selectedBuyInstrument()).toEqual(AAPL);
    expect(component.buyInstrumentStep.controls.instrumentInput.value).toBe('AAPL');
    expect(component.buyOrderStep.controls.priceLimit.value).toBe(100);
    expect(component.buyEstimate()).toBe(1000);
  });

  it('allows BUY continuation when the estimate does not exceed cash', () => {
    component.availableCash.set(1000);
    component.buyOrderStep.controls.quantity.setValue(2);
    component.buyOrderStep.controls.priceLimit.setValue(100);

    expect(component.buyDetailsInvalid()).toBe(false);

    component.buyOrderStep.controls.quantity.setValue(11);
    expect(component.buyDetailsInvalid()).toBe(true);
  });

  it('loads a SELL position and disables continuation beyond held quantity', () => {
    component.selectSellInstrument(optionEvent('AAPL') as never);
    component.sellOrderStep.controls.quantity.setValue(25);

    expect(positions.getPosition).toHaveBeenCalledWith('AAPL');
    expect(component.heldPosition()).toEqual(AAPL_POSITION);
    expect(component.sellDetailsInvalid()).toBe(false);

    component.sellOrderStep.controls.quantity.setValue(26);
    expect(component.sellDetailsInvalid()).toBe(true);
  });

  it('clears invalid instrument selections on input and blur', () => {
    component.selectBuyInstrument(optionEvent('AAPL') as never);
    component.onBuyInput();
    expect(component.selectedBuyInstrument()).toBeNull();
    component.buyInstrumentStep.controls.instrumentInput.setValue('unknown');
    component.onBuyBlur();
    expect(component.buyInstrumentStep.controls.instrumentInput.value).toBe('');

    component.selectSellInstrument(optionEvent('AAPL') as never);
    component.onSellInput();
    expect(component.selectedSellInstrument()).toBeNull();
    expect(component.heldPosition()).toBeNull();

    component.sellInstrumentStep.controls.instrumentInput.setValue('unknown');
    component.onSellBlur();
    expect(component.sellInstrumentStep.controls.instrumentInput.value).toBe('');
  });

  it('places a BUY order and reports success', () => {
    component.selectBuyInstrument(optionEvent('AAPL') as never);
    component.placeBuyOrder();

    expect(orders.placeOrder).toHaveBeenCalledWith(
      expect.objectContaining({
        accountId: ACCOUNT,
        symbol: 'AAPL',
        side: 'BUY',
        quantity: 10,
        priceLimit: 100,
      }),
    );
    expect(orders.placeOrder.mock.calls[0][0].idempotencyKey).toEqual(expect.any(String));
    expect(component.buyResponse()).toEqual(ORDER);
    expect(component.buyPlaced()).toBe(true);
  });

  it('reports BUY API errors and network failures', () => {
    orders.placeOrder.mockReturnValueOnce(
      throwError(
        () =>
          new HttpErrorResponse({
            status: 400,
            error: { message: 'Insufficient funds' },
          }),
      ),
    );
    component.selectBuyInstrument(optionEvent('AAPL') as never);
    component.placeBuyOrder();
    expect(component.buyPlaced()).toBe(false);
    expect(component.buyResultMessage()).toBe('Insufficient funds');

    orders.placeOrder.mockReturnValueOnce(throwError(() => new HttpErrorResponse({ status: 0 })));
    component.placeBuyOrder();
    expect(component.buyResultMessage()).toBe('Could not reach the order service.');
  });

  it('places a SELL order and reports success', () => {
    component.selectSellInstrument(optionEvent('AAPL') as never);
    component.placeSellOrder();

    expect(orders.placeOrder).toHaveBeenCalledWith(
      expect.objectContaining({
        accountId: ACCOUNT,
        symbol: 'AAPL',
        side: 'SELL',
        quantity: 10,
        priceLimit: 0.01,
      }),
    );
    expect(component.sellResponse()).toEqual(ORDER);
    expect(component.sellPlaced()).toBe(true);
  });

  it('reports SELL API errors and generic order failures', () => {
    positions.getPosition.mockReturnValueOnce(of(AAPL_POSITION));
    orders.placeOrder.mockReturnValueOnce(
      throwError(
        () =>
          new HttpErrorResponse({
            status: 400,
            error: { message: 'Not enough shares' },
          }),
      ),
    );
    component.selectSellInstrument(optionEvent('AAPL') as never);
    component.placeSellOrder();
    expect(component.sellPlaced()).toBe(false);
    expect(component.sellResultMessage()).toBe('Not enough shares');

    orders.placeOrder.mockReturnValueOnce(throwError(() => new Error('failed')));
    component.placeSellOrder();
    expect(component.sellResultMessage()).toBe('The order could not be placed. Please try again.');
  });

  it('reports instrument, account, and position loading errors', () => {
    instruments.getAllTradableInstruments.mockReturnValueOnce(
      throwError(() => new Error('instruments failed')),
    );
    accounts.getAccount.mockReturnValueOnce(throwError(() => new Error('account failed')));
    component.ngOnInit();
    expect(component.instruments()).toEqual([]);
    expect(component.instrumentsLoadError()).toBe('Could not load instruments from backend.');
    expect(component.accountLoadError()).toBe('Could not load available cash.');

    positions.getPosition.mockReturnValueOnce(throwError(() => new Error('position failed')));
    component.selectSellInstrument(optionEvent('TSLA') as never);
    expect(component.heldPosition()).toBeNull();
    expect(component.positionLoading()).toBe(false);
    expect(component.positionLoadError()).toBe('Could not load the held quantity.');
  });

  it('does not place an order without an account or selected instrument', () => {
    component.selectedBuyInstrument.set(null);
    component.placeBuyOrder();
    expect(orders.placeOrder).not.toHaveBeenCalled();
    expect(component.buyResultMessage()).toBe(
      'An account and instrument are required to place an order.',
    );

    user.set(null);
    component.selectedSellInstrument.set(AAPL);
    component.placeSellOrder();
    expect(orders.placeOrder).not.toHaveBeenCalled();
    expect(component.sellPlaced()).toBe(false);
  });

  it('closes the dialog', () => {
    component.close();
    expect(dialogRef.close).toHaveBeenCalledOnce();
  });
});

describe('OrderDialog portfolio choice', () => {
  let fixture: ComponentFixture<OrderDialog>;
  let component: OrderDialog;
  let orders: { placeOrder: ReturnType<typeof vi.fn> };
  let positions: { getPosition: ReturnType<typeof vi.fn> };
  let portfolios: { getPortfolios: ReturnType<typeof vi.fn> };

  const open = async (data: OrderDialogData | null) => {
    orders = { placeOrder: vi.fn(() => of(ORDER)) };
    positions = {
      getPosition: vi.fn((symbol: string, portfolioId?: string) =>
        of({
          ...AAPL_POSITION,
          portfolioId: portfolioId ?? null,
          quantity: portfolioId === 'PF0013' ? 3 : 25,
        }),
      ),
    };
    portfolios = { getPortfolios: vi.fn(() => of([MAIN, GOLDEN_APPLE])) };
    await TestBed.configureTestingModule({
      imports: [OrderDialog],
      providers: [
        {
          provide: Auth,
          useValue: { currentUser: signal<CurrentUser | null>(currentUser).asReadonly() },
        },
        {
          provide: Accounts,
          useValue: {
            getAccount: vi.fn(() =>
              of({ accountId: ACCOUNT, holderName: 'Trader', cashBalance: 1000, status: 'ACTIVE' }),
            ),
          },
        },
        {
          provide: Instruments,
          useValue: { getAllTradableInstruments: vi.fn(() => of([AAPL, TSLA])) },
        },
        { provide: Orders, useValue: orders },
        { provide: Positions, useValue: positions },
        { provide: Portfolios, useValue: portfolios },
        { provide: MatDialogRef, useValue: { close: vi.fn() } },
        { provide: MAT_DIALOG_DATA, useValue: data },
      ],
    }).compileComponents();
    fixture = TestBed.createComponent(OrderDialog);
    component = fixture.componentInstance;
    fixture.detectChanges();
  };
  const placedOrder = () => orders.placeOrder.mock.calls[0][0];

  it('defaults to Main when opened from the Orders page, and sends no portfolio for it', async () => {
    await open(null);

    expect(component.portfolios()).toEqual([MAIN, GOLDEN_APPLE]);
    expect(component.portfolioName()).toBe('Main');

    component.selectBuyInstrument(optionEvent('AAPL') as never);
    component.placeBuyOrder();

    expect(placedOrder()).not.toHaveProperty('portfolioId');
  });

  it('places the order in the portfolio the user picks', async () => {
    await open(null);
    component.selectBuyInstrument(optionEvent('AAPL') as never);

    component.selectPortfolio('PF0013');
    component.placeBuyOrder();

    expect(component.portfolioName()).toBe('Golden Apple');
    expect(placedOrder()).toEqual(expect.objectContaining({ side: 'BUY', portfolioId: 'PF0013' }));
  });

  it('re-checks the held quantity when the portfolio changes on a sell', async () => {
    await open(null);
    component.selectSellInstrument(optionEvent('AAPL') as never);
    expect(component.heldPosition()?.quantity).toBe(25);

    component.selectPortfolio('PF0013');

    expect(positions.getPosition).toHaveBeenLastCalledWith('AAPL', 'PF0013');
    expect(component.heldPosition()?.quantity).toBe(3);
  });

  it('starts a buy from a holding on that instrument and its portfolio', async () => {
    await open({
      side: 'BUY',
      symbol: 'AAPL',
      portfolioId: 'PF0013',
      portfolioName: 'Golden Apple',
    });

    expect(component.selectedTab()).toBe(0);
    expect(component.selectedBuyInstrument()).toEqual(AAPL);
    expect(component.buyOrderStep.controls.priceLimit.value).toBe(100);
    expect(component.portfolioName()).toBe('Golden Apple');

    component.placeBuyOrder();

    expect(placedOrder()).toEqual(
      expect.objectContaining({ symbol: 'AAPL', side: 'BUY', portfolioId: 'PF0013' }),
    );
  });

  it("starts a sell from a holding on the Sell tab, checking that portfolio's quantity", async () => {
    await open({ side: 'SELL', symbol: 'AAPL', portfolioId: 'PF0013' });

    expect(component.selectedTab()).toBe(1);
    expect(component.selectedSellInstrument()).toEqual(AAPL);
    expect(positions.getPosition).toHaveBeenCalledWith('AAPL', 'PF0013');
    expect(component.heldPosition()?.quantity).toBe(3);

    component.placeSellOrder();

    expect(placedOrder()).toEqual(
      expect.objectContaining({ symbol: 'AAPL', side: 'SELL', portfolioId: 'PF0013' }),
    );
  });

  it('a holding in Main sends no portfolio', async () => {
    await open({ side: 'BUY', symbol: 'AAPL', portfolioId: 'PF0011', portfolioName: 'Main' });

    component.placeBuyOrder();

    expect(placedOrder()).not.toHaveProperty('portfolioId');
  });

  it('still places orders if the portfolios cannot be loaded', async () => {
    await open(null);
    portfolios.getPortfolios.mockReturnValue(throwError(() => new Error('down')));
    component.ngOnInit();
    component.selectBuyInstrument(optionEvent('AAPL') as never);

    component.placeBuyOrder();

    expect(component.portfolios()).toEqual([]);
    expect(component.portfolioName()).toBe('Main');
    expect(placedOrder()).not.toHaveProperty('portfolioId');
  });
});
