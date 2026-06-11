import { Component, input, output } from '@angular/core';
import { RouterLink } from '@angular/router';

@Component({
  selector: 'app-navbar',
  standalone: true,
  imports: [RouterLink],
  templateUrl: './navbar.component.html',
  styleUrl: './navbar.component.scss',
})
export class NavbarComponent {
  readonly sidebarOpen = input(false);
  menuToggled = output<void>();

  readonly stats = {
    xp: '2,450',
    rank: 'Gold III',
  };

  onMenuToggle(): void {
    this.menuToggled.emit();
  }
}
