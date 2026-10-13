import { Component, inject } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { Positions } from '../../../services/positions';

@Component({
  imports: [],
  selector: 'app-positions-list',
  styleUrl: './positions-list.scss',
  templateUrl: './positions-list.html',
})
export class PositionsList {
  protected readonly service = inject(Positions);
  protected readonly positions = toSignal(this.service.getPositions(), { initialValue: [] });
}
  