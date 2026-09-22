import {
  APP_INITIALIZER,
  ApplicationConfig,
  provideZoneChangeDetection,
} from '@angular/core';
import { provideRouter, withComponentInputBinding } from '@angular/router';
import { provideHttpClient, withInterceptors, withXsrfConfiguration } from '@angular/common/http';
import { provideAnimationsAsync } from '@angular/platform-browser/animations/async';
import { definePreset } from '@primeng/themes';
import { providePrimeNG } from 'primeng/config';
import Aura from '@primeng/themes/aura';
import { firstValueFrom } from 'rxjs';

import { routes } from './app.routes';
import { authInterceptor } from './core/interceptors/auth.interceptor';
import { AuthService } from './core/services/auth.service';
import { LocaleService } from './core/services/locale.service';

/** Aura retinted to SuccessHub gold so PrimeNG widgets match the app palette. */
const SuccesHubAura = definePreset(Aura, {
  semantic: {
    primary: {
      50: '#fbf6e8',
      100: '#f6ebc8',
      200: '#f0d991',
      300: '#ecc86a',
      400: '#f6cf63',
      500: '#e8b33e',
      600: '#d29f22',
      700: '#a87c18',
      800: '#6e561f',
      900: '#4f3900',
      950: '#402d00',
    },
  },
});

export const appConfig: ApplicationConfig = {
  providers: [
    provideZoneChangeDetection({ eventCoalescing: true }),
    provideRouter(routes, withComponentInputBinding()),
    provideHttpClient(
      withInterceptors([authInterceptor]),
      withXsrfConfiguration({
        cookieName: 'XSRF-TOKEN',
        headerName: 'X-XSRF-TOKEN',
      })
    ),
    provideAnimationsAsync(),
    providePrimeNG({
      theme: {
        preset: SuccesHubAura,
        options: {
          darkModeSelector: '.app-dark',
          cssLayer: {
            name: 'primeng',
            order: 'theme, base, primeng',
          },
        },
      },
    }),
    {
      provide: APP_INITIALIZER,
      multi: true,
      deps: [LocaleService],
      useFactory: (locale: LocaleService) => () => locale.init(),
    },
    {
      provide: APP_INITIALIZER,
      multi: true,
      deps: [AuthService],
      useFactory: (auth: AuthService) => () =>
        firstValueFrom(auth.loadCurrentUser()),
    },
  ],
};
