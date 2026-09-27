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
import { TranslatePipe } from '../../../core/i18n/translate.pipe';
import { TranslateService } from '../../../core/i18n/translate.service';
import { RewardLabelPipe } from '../../../core/i18n/reward-label.pipe';

const BOX_NAME_BY_EN: Record<string, string> = {
  'Glowing Orb': 'ARCANE_ORB',
  'Treasure Chest': 'IRON_CHEST',
  'Divine Vault': 'SOVEREIGN_VAULT',
};

const LOOT_REASON_BY_EN: Record<string, string> = {
  'an achievement unlock': 'achievement',
  'your weekly ritual': 'weekly',
  'a grant': 'grant',
  'your progress': 'progress',
};

/** English API labels → achievement keys (notification bodies store the English label). */
const ACHIEVEMENT_KEY_BY_EN_LABEL: Record<string, string> = {
  Speedster: 'SPEEDSTER',
  Pioneer: 'PIONEER',
  Archivist: 'ARCHIVIST',
  Consistent: 'CONSISTENT',
  Sovereign: 'SOVEREIGN',
};

@Component({
  selector: 'app-navbar',
  standalone: true,
  imports: [RouterLink, DecimalPipe, FrameOrnamentsComponent, TranslatePipe, RewardLabelPipe],
  templateUrl: './navbar.component.html',
  styleUrl: './navbar.component.scss',
})
export class NavbarComponent implements OnInit {
  protected readonly profileService = inject(ProfileService);
  protected readonly auth = inject(AuthService);
  protected readonly cosmetics = inject(EquippedCosmeticsService);
  protected readonly notifications = inject(NotificationService);
  private readonly i18n = inject(TranslateService);
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

  notificationTitle(row: AppNotification): string {
    const key = `notifications.title.${row.type}`;
    const translated = this.i18n.t(key);
    return translated === key ? row.title : translated;
  }

  notificationBody(row: AppNotification): string {
    switch (row.type) {
      case 'BOSS_DEFEATED': {
        const m = /^(.*) has been slain\.$/.exec(row.body);
        if (m) {
          return this.i18n.t('notifications.bossSlain', { name: m[1] });
        }
        break;
      }
      case 'STREAK_MILESTONE': {
        const m = /^You earned a (.*) for your streak\.$/.exec(row.body);
        if (m) {
          return this.i18n.t('notifications.streakLoot', { box: this.localizeBoxName(m[1]) });
        }
        break;
      }
      case 'LOOT_EARNED': {
        const m = /^A (.*) awaits from (.*)\.$/.exec(row.body);
        if (m) {
          return this.i18n.t('notifications.lootAwaits', {
            box: this.localizeBoxName(m[1]),
            reason: this.localizeLootReason(m[2]),
          });
        }
        break;
      }
      case 'ACHIEVEMENT_UNLOCKED': {
        const key = ACHIEVEMENT_KEY_BY_EN_LABEL[row.body];
        if (key) {
          return this.i18n.catalogEntry('achievements', key, 'label', row.body);
        }
        break;
      }
      default:
        break;
    }
    return row.body;
  }

  relativeTime(iso: string): string {
    const then = Date.parse(iso);
    if (Number.isNaN(then)) {
      return '';
    }
    const seconds = Math.round((Date.now() - then) / 1000);
    if (seconds < 60) {
      return this.i18n.t('navbar.justNow');
    }
    const minutes = Math.round(seconds / 60);
    if (minutes < 60) {
      return this.i18n.t('navbar.minutesAgo', { n: minutes });
    }
    const hours = Math.round(minutes / 60);
    if (hours < 48) {
      return this.i18n.t('navbar.hoursAgo', { n: hours });
    }
    const days = Math.round(hours / 24);
    return this.i18n.t('navbar.daysAgo', { n: days });
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

  private localizeBoxName(englishName: string): string {
    const id = BOX_NAME_BY_EN[englishName];
    if (!id) {
      return englishName;
    }
    const key = `loot.boxes.${id}.name`;
    const translated = this.i18n.t(key);
    return translated === key ? englishName : translated;
  }

  private localizeLootReason(englishReason: string): string {
    const reasonKey = LOOT_REASON_BY_EN[englishReason];
    if (!reasonKey) {
      return englishReason;
    }
    return this.i18n.t(`notifications.reason.${reasonKey}`);
  }
}
