import { Injectable, signal, computed } from '@angular/core';
import { Router } from '@angular/router';

export interface User {
  id: string;
  name: string;
  email: string;
  avatarUrl?: string;
}

@Injectable({
  providedIn: 'root',
})
export class AuthService {
  private readonly _currentUser = signal<User | null>(null);

  readonly currentUser = this._currentUser.asReadonly();
  readonly isLoggedIn = computed(() => this._currentUser() !== null);
  readonly userInitials = computed(() => {
    const user = this._currentUser();
    if (!user) return '';
    return user.name
      .split(' ')
      .map((n) => n[0])
      .join('')
      .toUpperCase()
      .slice(0, 2);
  });

  constructor(private router: Router) {
    // Load user from storage on init
    const stored = localStorage.getItem('current_user');
    if (stored) {
      try {
        this._currentUser.set(JSON.parse(stored));
      } catch {
        localStorage.removeItem('current_user');
      }
    }
  }

  login(user: User, token: string): void {
    localStorage.setItem('access_token', token);
    localStorage.setItem('current_user', JSON.stringify(user));
    this._currentUser.set(user);
  }

  logout(): void {
    localStorage.removeItem('access_token');
    localStorage.removeItem('current_user');
    this._currentUser.set(null);
    this.router.navigate(['/']);
  }
}
