import { Injectable, inject } from '@angular/core';
import { ApiService } from './api.service';
import { Observable } from 'rxjs';
import { DailyQuote } from '../models/lunar.model';

@Injectable({
  providedIn: 'root',
})
export class QuoteService {
  private readonly api = inject(ApiService);

  getDailyQuote(): Observable<DailyQuote> {
    return this.api.get<DailyQuote>('/quote/daily');
  }
}
