import { CommonModule } from '@angular/common';
import { toSignal } from '@angular/core/rxjs-interop';
import { Component, OnInit, ViewEncapsulation, computed, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatAutocompleteModule, MatAutocompleteSelectedEvent } from '@angular/material/autocomplete';
import { MatButtonModule } from '@angular/material/button';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSliderModule } from '@angular/material/slider';
import { MatStepperModule } from '@angular/material/stepper';
import { MatTabsModule } from '@angular/material/tabs';
import { Auth } from '../../../services/auth/auth';
import { HttpErrorResponse } from '@angular/common/http';
import { Accounts } from '../../../services/accounts';
import { Instruments } from '../../../services/instruments';
import { TradingInstrument } from '../../../services/interfaces/instruments.interfaces';
import { OrderResponse } from '../../../services/interfaces/orders.interfaces';
import { Orders } from '../../../services/orders';
import { Position, Positions } from '../../../services/positions';
import { startWith } from 'rxjs';

type Instrument = TradingInstrument;

export interface OrderDialogData {
  side?: 'BUY' | 'SELL';
  symbol?: string;
  portfolioId?: string;
  portfolioName?: string;
}

@Component({
  imports: [
    CommonModule,
    ReactiveFormsModule,
    MatAutocompleteModule,
    MatButtonModule,
    MatDialogModule,
    MatFormFieldModule,
    MatInputModule,
    MatSliderModule,
    MatStepperModule,
    MatTabsModule,
  ],
  selector: 'app-order-dialog',
  styleUrl: './order-dialog.scss',
  templateUrl: './order-dialog.html',
  encapsulation: ViewEncapsulation.None,
})
export class OrderDialog implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly dialogRef = inject(MatDialogRef<OrderDialog>);
  private readonly auth = inject(Auth);
  private readonly accountsApi = inject(Accounts);
  private readonly instrumentsApi = inject(Instruments);
  private readonly ordersApi = inject(Orders);
  private readonly positionsApi = inject(Positions);
  readonly data = inject<OrderDialogData | null>(MAT_DIALOG_DATA, { optional: true });

  readonly selectedTab = signal(0);

  readonly instruments = signal<Instrument[]>([]);
  readonly instrumentsLoading = signal(false);
  readonly instrumentsLoadError = signal<string | null>(null);
  readonly availableCash = signal<number | null>(null);
  readonly accountLoadError = signal<string | null>(null);
  readonly heldPosition = signal<Position | null>(null);
  readonly positionLoading = signal(false);
  readonly positionLoadError = signal<string | null>(null);

  readonly buyInstrumentStep = this.fb.group({
    instrumentInput: ['', Validators.required],
  });

  readonly buyOrderStep = this.fb.group({
    quantity: [10, [Validators.required, Validators.min(1)]],
    priceLimit: [0, [Validators.required, Validators.min(0.01)]],
  });

  readonly sellInstrumentStep = this.fb.group({
    instrumentInput: ['', Validators.required],
  });

  readonly sellOrderStep = this.fb.group({
    quantity: [10, [Validators.required, Validators.min(1)]],
  });

  readonly buyPlaced = signal<boolean | null>(null);
  readonly sellPlaced = signal<boolean | null>(null);
  readonly buySubmitting = signal(false);
  readonly sellSubmitting = signal(false);
  readonly buyResponse = signal<OrderResponse | null>(null);
  readonly sellResponse = signal<OrderResponse | null>(null);
  readonly buyResultMessage = signal<string | null>(null);
  readonly sellResultMessage = signal<string | null>(null);

  readonly selectedBuyInstrument = signal<Instrument | null>(null);
  readonly selectedSellInstrument = signal<Instrument | null>(null);

  readonly buyInstrumentInput = toSignal(
    this.buyInstrumentStep.controls.instrumentInput.valueChanges.pipe(startWith(this.buyInstrumentStep.controls.instrumentInput.value || '')),
    { initialValue: this.buyInstrumentStep.controls.instrumentInput.value || '' },
  );

  readonly sellInstrumentInput = toSignal(
    this.sellInstrumentStep.controls.instrumentInput.valueChanges.pipe(startWith(this.sellInstrumentStep.controls.instrumentInput.value || '')),
    { initialValue: this.sellInstrumentStep.controls.instrumentInput.value || '' },
  );

  readonly buyQuantityInput = toSignal(
    this.buyOrderStep.controls.quantity.valueChanges.pipe(startWith(this.buyOrderStep.controls.quantity.value || 0)),
    { initialValue: this.buyOrderStep.controls.quantity.value || 0 },
  );

  readonly buyPriceLimitInput = toSignal(
    this.buyOrderStep.controls.priceLimit.valueChanges.pipe(startWith(this.buyOrderStep.controls.priceLimit.value || 0)),
    { initialValue: this.buyOrderStep.controls.priceLimit.value || 0 },
  );

  readonly sellQuantityInput = toSignal(
    this.sellOrderStep.controls.quantity.valueChanges.pipe(startWith(this.sellOrderStep.controls.quantity.value || 0)),
    { initialValue: this.sellOrderStep.controls.quantity.value || 0 },
  );

  readonly buyOrderValid = toSignal(
    this.buyOrderStep.statusChanges.pipe(startWith(this.buyOrderStep.valid)),
    { initialValue: this.buyOrderStep.valid },
  );

  readonly sellOrderValid = toSignal(
    this.sellOrderStep.statusChanges.pipe(startWith(this.sellOrderStep.valid)),
    { initialValue: this.sellOrderStep.valid },
  );

  readonly buyMatches = computed(() => this.filterInstruments(String(this.buyInstrumentInput() || '')));
  readonly sellMatches = computed(() => this.filterInstruments(String(this.sellInstrumentInput() || '')));

  readonly buyEstimate = computed(() => {
    const qty = Number(this.buyQuantityInput() || 0);
    const limit = Number(this.buyPriceLimitInput() || 0);
    return qty * limit;
  });

  readonly sellEstimate = computed(() => {
    const qty = Number(this.sellQuantityInput() || 0);
    const instrument = this.selectedSellInstrument();
    return qty * (instrument?.marketPrice || 0);
  });

  readonly buyDetailsInvalid = computed(() => {
    const cash = this.availableCash();
    return !this.buyOrderValid() || cash === null || this.buyEstimate() > cash;
  });

  readonly sellDetailsInvalid = computed(() => {
    const position = this.heldPosition();
    const quantity = Number(this.sellQuantityInput() || 0);
    return !this.sellOrderValid()
      || this.positionLoading()
      || position === null
      || quantity > (position?.quantity ?? 0);
  });

  displayInstrument = (instrument: Instrument | string | null): string => {
    if (!instrument) {
      return '';
    }
    if (typeof instrument === 'string') {
      return instrument;
    }
    return `${instrument.symbol} - ${instrument.name}`;
  };

  setTab(index: number): void {
    this.selectedTab.set(index);
  }

  ngOnInit(): void {
    if (this.data?.side === 'SELL') {
      this.selectedTab.set(1);
    }
    this.loadInstruments();
    this.loadAvailableCash();
  }

  selectBuyInstrument(event: MatAutocompleteSelectedEvent): void {
    const symbol = String(event.option.value);
    const selected = this.instruments().find((instrument) => instrument.symbol === symbol) || null;
    this.selectedBuyInstrument.set(selected);
    this.buyInstrumentStep.controls.instrumentInput.setValue(symbol);
    if (selected && this.buyOrderStep.controls.priceLimit.value === 0) {
      this.buyOrderStep.controls.priceLimit.setValue(selected.marketPrice);
    }
  }

  selectSellInstrument(event: MatAutocompleteSelectedEvent): void {
    const symbol = String(event.option.value);
    const selected = this.instruments().find((instrument) => instrument.symbol === symbol) || null;
    this.selectedSellInstrument.set(selected);
    this.sellInstrumentStep.controls.instrumentInput.setValue(symbol);
    this.loadHeldPosition(symbol);
  }

  onBuyInput(): void {
    this.selectedBuyInstrument.set(null);
  }

  onSellInput(): void {
    this.selectedSellInstrument.set(null);
    this.heldPosition.set(null);
    this.positionLoadError.set(null);
  }

  onBuyBlur(): void {
    const selected = this.selectedBuyInstrument();
    const current = String(this.buyInstrumentStep.controls.instrumentInput.value || '').trim();
    if (!selected || current !== selected.symbol) {
      this.selectedBuyInstrument.set(null);
      this.buyInstrumentStep.controls.instrumentInput.setValue('');
    }
  }

  onSellBlur(): void {
    const selected = this.selectedSellInstrument();
    const current = String(this.sellInstrumentStep.controls.instrumentInput.value || '').trim();
    if (!selected || current !== selected.symbol) {
      this.selectedSellInstrument.set(null);
      this.sellInstrumentStep.controls.instrumentInput.setValue('');
    }
  }

  placeBuyOrder(): void {
    const instrument = this.selectedBuyInstrument();
    const accountId = this.auth.currentUser()?.accountId;
    if (!instrument || !accountId) {
      this.buyPlaced.set(false);
      this.buyResultMessage.set('An account and instrument are required to place an order.');
      return;
    }

    this.buyPlaced.set(null);
    this.buySubmitting.set(true);
    this.buyResultMessage.set(null);
    this.ordersApi.placeOrder({
      accountId,
      symbol: instrument.symbol,
      side: 'BUY',
      quantity: Number(this.buyOrderStep.controls.quantity.value),
      priceLimit: Number(this.buyOrderStep.controls.priceLimit.value),
      idempotencyKey: this.createIdempotencyKey(),
      ...this.portfolio(),
    }).subscribe({
      next: (response) => {
        this.buyResponse.set(response);
        this.buyPlaced.set(true);
        this.buySubmitting.set(false);
        this.buyResultMessage.set(`Order ${response.orderId} was placed with status ${response.orderStatus}.`);
      },
      error: (error: unknown) => {
        this.buyPlaced.set(false);
        this.buySubmitting.set(false);
        this.buyResultMessage.set(this.orderErrorMessage(error));
      },
    });
  }

  placeSellOrder(): void {
    const instrument = this.selectedSellInstrument();
    const accountId = this.auth.currentUser()?.accountId;
    if (!instrument || !accountId) {
      this.sellPlaced.set(false);
      this.sellResultMessage.set('An account and instrument are required to place an order.');
      return;
    }

    this.sellPlaced.set(null);
    this.sellSubmitting.set(true);
    this.sellResultMessage.set(null);
    this.ordersApi.placeOrder({
      accountId,
      symbol: instrument.symbol,
      side: 'SELL',
      quantity: Number(this.sellOrderStep.controls.quantity.value),
      priceLimit: 0.01,
        idempotencyKey: this.createIdempotencyKey(),
      ...this.portfolio(),
    }).subscribe({
      next: (response) => {
        this.sellResponse.set(response);
        this.sellPlaced.set(true);
          this.sellSubmitting.set(false);
        this.sellResultMessage.set(`Order ${response.orderId} was placed with status ${response.orderStatus}.`);
      },
      error: (error: unknown) => {
        this.sellPlaced.set(false);
          this.sellSubmitting.set(false);
        this.sellResultMessage.set(this.orderErrorMessage(error));
      },
    });
  }

  close(): void {
    this.dialogRef.close();
  }

  private filterInstruments(query: string): Instrument[] {
    const items = this.instruments();
    const normalized = query.toLowerCase().trim();
    if (!normalized) {
      return items;
    }
    return items.filter((instrument) => {
      return (
        instrument.symbol.toLowerCase().includes(normalized)
        || instrument.name.toLowerCase().includes(normalized)
        || instrument.type.toLowerCase().includes(normalized)
      );
    });
  }

  private loadInstruments(): void {
    this.instrumentsLoading.set(true);
    this.instrumentsLoadError.set(null);

    this.instrumentsApi.getAllTradableInstruments().subscribe({
      next: (items) => {
        this.instruments.set(items);
        this.instrumentsLoading.set(false);
        this.preselectInstrument();
      },
      error: () => {
        this.instruments.set([]);
        this.instrumentsLoadError.set('Could not load instruments from backend.');
        this.instrumentsLoading.set(false);
      },
    });
  }

  private loadAvailableCash(): void {
    const accountId = this.auth.currentUser()?.accountId;
    if (!accountId) {
      this.accountLoadError.set('No trading account is linked to this user.');
      return;
    }

    this.accountsApi.getAccount(accountId).subscribe({
      next: (account) => {
        this.availableCash.set(Number(account.cashBalance));
      },
      error: () => {
        this.accountLoadError.set('Could not load available cash.');
      },
    });
  }

  private loadHeldPosition(symbol: string): void {
    this.positionLoading.set(true);
    this.positionLoadError.set(null);
    this.heldPosition.set(null);
    const portfolioId = this.data?.portfolioId;
    const position$ = portfolioId ? this.positionsApi.getPosition(symbol, portfolioId) : this.positionsApi.getPosition(symbol);
    position$.subscribe({
      next: (position) => {
        this.heldPosition.set(position);
        this.positionLoading.set(false);
      },
      error: () => {
        this.positionLoadError.set('Could not load the held quantity.');
        this.positionLoading.set(false);
      },
    });
  }

  private preselectInstrument(): void {
    const symbol = this.data?.symbol;
    if (!symbol || !this.instruments().some((instrument) => instrument.symbol === symbol)) {
      return;
    }
    const event = { option: { value: symbol } } as MatAutocompleteSelectedEvent;
    if (this.data?.side === 'SELL') {
      this.selectSellInstrument(event);
    } else {
      this.selectBuyInstrument(event);
    }
  }

  private portfolio(): { portfolioId?: string } {
    return this.data?.portfolioId ? { portfolioId: this.data.portfolioId } : {};
  }

  private orderErrorMessage(error: unknown): string {
    if (error instanceof HttpErrorResponse) {
      const message = (error.error as { message?: unknown } | null)?.message;
      if (typeof message === 'string' && message.trim()) {
        return message;
      }
      if (error.status === 0) {
        return 'Could not reach the order service.';
      }
    }
    return 'The order could not be placed. Please try again.';
  }

  private createIdempotencyKey(): string {
    const browserCrypto = globalThis.crypto;
    if (typeof browserCrypto?.randomUUID === 'function') {
      return browserCrypto.randomUUID();
    }
    return `${Date.now()}-${Math.random().toString(36).slice(2)}`;
  }
}
