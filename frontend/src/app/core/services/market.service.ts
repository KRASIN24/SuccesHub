import { Injectable, inject } from '@angular/core';
import { ApiService } from './api.service';
import { Observable } from 'rxjs';
import { MarketData } from '../models/market.model';

@Injectable({
  providedIn: 'root',
})
export class MarketService {
  private readonly api = inject(ApiService);

  getPrices(): Observable<MarketData> {
    return this.api.get<MarketData>('/market/prices');
  }
}
