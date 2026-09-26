import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';

export interface AccountStatus {
  email: string | null;
  mfaEnabled: boolean;
  deactivated: boolean;
  purgeAt: string | null;
}

export interface MfaSetupResponse {
  redirectUrl: string;
}

/**
 * BFF account self-service (email / password / MFA / deactivate / reactivate).
 */
@Injectable({
  providedIn: 'root',
})
export class AccountService {
  private readonly http = inject(HttpClient);
  private readonly base = `${environment.apiUrl}/account`;

  getStatus(): Observable<AccountStatus> {
    return this.http.get<AccountStatus>(this.base);
  }

  updateEmail(email: string): Observable<void> {
    return this.http.post<void>(`${this.base}/email`, { email });
  }

  changePassword(currentPassword: string, newPassword: string): Observable<void> {
    return this.http.post<void>(`${this.base}/password`, { currentPassword, newPassword });
  }

  mfaSetup(): Observable<MfaSetupResponse> {
    return this.http.get<MfaSetupResponse>(`${this.base}/mfa/setup`);
  }

  deleteAccount(confirmation: string): Observable<void> {
    return this.http.delete<void>(this.base, { body: { confirmation } });
  }

  reactivate(): Observable<void> {
    return this.http.post<void>(`${this.base}/reactivate`, {});
  }
}
