import { Injectable, inject } from '@angular/core';
import { ApiService } from './api.service';
import { Observable } from 'rxjs';
import { LunarData } from '../models/lunar.model';

@Injectable({
  providedIn: 'root',
})
export class AstronomyService {
  private readonly api = inject(ApiService);

  getLunarPhase(): Observable<LunarData> {
    return this.api.get<LunarData>('/astronomy/lunar');
  }
}
