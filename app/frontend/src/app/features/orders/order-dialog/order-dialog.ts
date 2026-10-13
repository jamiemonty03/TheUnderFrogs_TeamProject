import { CommonModule } from '@angular/common';
import { toSignal } from '@angular/core/rxjs-interop';
import { Component, OnInit, ViewEncapsulation, computed, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatAutocompleteModule, MatAutocompleteSelectedEvent } from '@angular/material/autocomplete';
import { MatButtonModule } from '@angular/material/button';
import { MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSliderModule } from '@angular/material/slider';
import { MatStepperModule } from '@angular/material/stepper';
import { MatTabsModule } from '@angular/material/tabs';
import { Instruments, TradingInstrument } from '../../../services/instruments';
import { startWith } from 'rxjs';

type Instrument = TradingInstrument;

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
  private readonly instrumentsApi = inject(Instruments);

  readonly selectedTab = signal(0);

  readonly instruments = signal<Instrument[]>([]);
  readonly instrumentsLoading = signal(false);
  readonly instrumentsLoadError = signal<string | null>(null);

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
    this.loadInstruments();
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
  }

  onBuyInput(): void {
    this.selectedBuyInstrument.set(null);
  }

  onSellInput(): void {
    this.selectedSellInstrument.set(null);
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
    this.buyPlaced.set(Math.random() > 0.15);
  }

  placeSellOrder(): void {
    this.sellPlaced.set(Math.random() > 0.15);
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
      },
      error: () => {
        this.instruments.set([]);
        this.instrumentsLoadError.set('Could not load instruments from backend.');
        this.instrumentsLoading.set(false);
      },
    });
  }
}
