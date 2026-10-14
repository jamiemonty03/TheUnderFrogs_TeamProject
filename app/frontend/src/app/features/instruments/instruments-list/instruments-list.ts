import { Component } from '@angular/core';
import { MarketOverview } from '../market-overview/market-overview';

@Component({
  imports: [MarketOverview],
  selector: 'app-instruments-list',
  styleUrl: './instruments-list.scss',
  templateUrl: './instruments-list.html',
})
export class InstrumentsList {}
