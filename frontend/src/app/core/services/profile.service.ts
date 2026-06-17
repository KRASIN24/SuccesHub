import { Injectable, inject } from '@angular/core';
import { ApiService } from './api.service';
import { Observable } from 'rxjs';
import { UserProfile } from '../models/profile.model';

@Injectable({
  providedIn: 'root',
})
export class ProfileService {
  private readonly api = inject(ApiService);

  getProfile(): Observable<UserProfile> {
    return this.api.get<UserProfile>('/profile');
  }
}
