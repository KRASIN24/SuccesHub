import { Component, OnInit, inject, input, output } from '@angular/core';
import { DecimalPipe } from '@angular/common';
import { RouterLink } from '@angular/router';
import { ProfileService } from '../../../core/services/profile.service';

@Component({
  selector: 'app-navbar',
  standalone: true,
  imports: [RouterLink, DecimalPipe],
  templateUrl: './navbar.component.html',
  styleUrl: './navbar.component.scss',
})
export class NavbarComponent implements OnInit {
  protected readonly profileService = inject(ProfileService);

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
