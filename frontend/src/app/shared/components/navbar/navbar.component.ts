import { Component, OnInit, inject, input, output } from '@angular/core';
import { DecimalPipe } from '@angular/common';
import { RouterLink } from '@angular/router';
import { ProfileService } from '../../../core/services/profile.service';
import { AuthService } from '../../../core/services/auth.service';
import { EquippedCosmeticsService } from '../../../core/services/equipped-cosmetics.service';
import { FrameOrnamentsComponent } from '../frame-ornaments/frame-ornaments.component';

@Component({
  selector: 'app-navbar',
  standalone: true,
  imports: [RouterLink, DecimalPipe, FrameOrnamentsComponent],
  templateUrl: './navbar.component.html',
  styleUrl: './navbar.component.scss',
})
export class NavbarComponent implements OnInit {
  protected readonly profileService = inject(ProfileService);
  protected readonly auth = inject(AuthService);
  protected readonly cosmetics = inject(EquippedCosmeticsService);

  readonly sidebarOpen = input(false);
  menuToggled = output<void>();

  ngOnInit(): void {
    if (!this.profileService.profile()) {
      this.profileService.getProfile().subscribe({
        error: (err) => console.error('Failed to load navbar profile', err),
      });
    }
  }

  onMenuToggle(): void {
    this.menuToggled.emit();
  }
}
