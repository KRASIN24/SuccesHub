import {
  Component,
  ElementRef,
  HostListener,
  OnInit,
  inject,
  input,
  output,
  signal,
} from '@angular/core';
import { DecimalPipe } from '@angular/common';
import { Router, RouterLink } from '@angular/router';
import { ProfileService } from '../../../core/services/profile.service';
import { AuthService } from '../../../core/services/auth.service';
import { EquippedCosmeticsService } from '../../../core/services/equipped-cosmetics.service';
import { NotificationService } from '../../../core/services/notification.service';
import { AppNotification } from '../../../core/models/notification.model';
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
  protected readonly notifications = inject(NotificationService);
  private readonly router = inject(Router);
  private readonly host = inject(ElementRef<HTMLElement>);

  readonly sidebarOpen = input(false);
  menuToggled = output<void>();

  readonly panelOpen = signal(false);

  ngOnInit(): void {
    if (!this.profileService.profile()) {
      this.profileService.getProfile().subscribe({
        error: (err) => console.error('Failed to load navbar profile', err),
      });
    }
    this.notifications.refresh();
  }

  onMenuToggle(): void {
    this.menuToggled.emit();
  }

  togglePanel(event: MouseEvent): void {
    event.stopPropagation();
    const next = !this.panelOpen();
    this.panelOpen.set(next);
    if (next) {
      this.notifications.refresh();
    }
  }

  closePanel(): void {
    this.panelOpen.set(false);
  }

  @HostListener('document:click', ['$event'])
  onDocumentClick(event: MouseEvent): void {
    if (!this.panelOpen()) {
      return;
    }
    if (!this.host.nativeElement.contains(event.target as Node)) {
      this.closePanel();
    }
  }

  @HostListener('document:keydown.escape')
  onEscape(): void {
    this.closePanel();
  }

  markAllRead(event: MouseEvent): void {
    event.stopPropagation();
    this.notifications.markAllRead().subscribe({
      error: (err) => console.error('Failed to mark all notifications read', err),
    });
  }

  openNotification(row: AppNotification, event: MouseEvent): void {
    event.stopPropagation();
    const navigate = () => {
      this.closePanel();
      if (row.actionUrl) {
        void this.router.navigateByUrl(row.actionUrl);
      }
    };
    if (row.read) {
      navigate();
      return;
    }
    this.notifications.markRead(row.id).subscribe({
      next: () => navigate(),
      error: (err) => {
        console.error('Failed to mark notification read', err);
        navigate();
      },
    });
  }

  relativeTime(iso: string): string {
    const then = Date.parse(iso);
    if (Number.isNaN(then)) {
      return '';
    }
    const seconds = Math.round((Date.now() - then) / 1000);
    if (seconds < 60) {
      return 'just now';
    }
    const minutes = Math.round(seconds / 60);
    if (minutes < 60) {
      return `${minutes}m ago`;
    }
    const hours = Math.round(minutes / 60);
    if (hours < 48) {
      return `${hours}h ago`;
    }
    const days = Math.round(hours / 24);
    return `${days}d ago`;
  }

  iconFor(type: string): string {
    switch (type) {
      case 'LOOT_EARNED':
      case 'STREAK_MILESTONE':
        return 'inventory_2';
      case 'ACHIEVEMENT_UNLOCKED':
        return 'military_tech';
      case 'BOSS_DEFEATED':
        return 'swords';
      default:
        return 'notifications';
    }
  }
}
