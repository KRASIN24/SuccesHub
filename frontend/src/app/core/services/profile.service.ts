import { Injectable, inject, signal } from '@angular/core';
import { ApiService } from './api.service';
import { Observable, tap } from 'rxjs';
import { UserProfile } from '../models/profile.model';

@Injectable({
  providedIn: 'root',
})
export class ProfileService {
  private readonly api = inject(ApiService);
  private readonly profileState = signal<UserProfile | null>(null);

  /** Live profile shared across navbar, sidebar, and gamification flows. */
  readonly profile = this.profileState.asReadonly();

  getProfile(): Observable<UserProfile> {
    return this.api.get<UserProfile>('/profile').pipe(tap((profile) => this.profileState.set(profile)));
  }

  applyProfile(profile: UserProfile): void {
    this.profileState.set(profile);
  }

  refreshProfile(): Observable<UserProfile> {
    return this.getProfile();
  }
}
