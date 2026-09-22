import { CommonModule } from '@angular/common';
import { Component, OnInit, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { NavigationEnd, Router } from '@angular/router';
import { filter } from 'rxjs';
import { AuthService } from '../../../core/services/auth.service';
import { DevToolId, DevToolsService } from '../../../core/services/dev-tools.service';
import { GamificationService } from '../../../core/services/gamification.service';
import { LootPendingService } from '../../../core/services/loot-pending.service';

const TOOL_META: Record<
  DevToolId,
  { label: string; icon: string; title: string }
> = {
  streak: { label: 'Streak', icon: 'local_fire_department', title: 'Streak tools' },
  cache: { label: 'Cache', icon: 'inventory_2', title: 'Cache tools' },
  time: { label: 'Time', icon: 'schedule', title: 'Virtual clock' },
  ritual: { label: 'Ritual', icon: 'auto_awesome', title: 'Ritual tools' },
};

/**
 * Global floating Dev dock + expanded messenger panel for gated testing tools.
 */
@Component({
  selector: 'app-dev-tools-host',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './dev-tools-host.component.html',
  styleUrl: './dev-tools-host.component.scss',
})
export class DevToolsHostComponent implements OnInit {
  readonly tools = inject(DevToolsService);
  private readonly gamification = inject(GamificationService);
  private readonly lootPending = inject(LootPendingService);
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);

  readonly toolMeta = TOOL_META;
  readonly toolIds: DevToolId[] = ['streak', 'cache', 'time', 'ritual'];

  ngOnInit(): void {
    this.tools.refresh();
    this.router.events.pipe(filter((e) => e instanceof NavigationEnd)).subscribe(() => {
      if (this.auth.isLoggedIn()) {
        this.tools.refresh();
      }
    });
  }

  toggleDock(): void {
    if (this.tools.openTool()) {
      this.tools.close();
    } else {
      this.tools.open('time');
    }
  }

  selectTool(id: DevToolId): void {
    this.tools.open(id);
  }

  closePanel(): void {
    this.tools.close();
  }

  adjustMinusOne(): void {
    this.adjustStreak(-1);
  }

  adjustPlusOne(): void {
    this.adjustStreak(1);
  }

  adjustStreak(delta: number): void {
    this.run(() => this.gamification.adjustStreak(delta), (r) => {
      this.tools.applyStreakResult(r.currentStreak);
      return r.message;
    }, true);
  }

  applySetStreak(): void {
    this.run(
      () => this.gamification.setStreak(Number(this.tools.streakSetValue()) || 0),
      (r) => {
        this.tools.applyStreakResult(r.currentStreak);
        return r.message;
      },
      true
    );
  }

  requestResetStreak(): void {
    this.tools.pendingConfirm.set('reset-streak');
  }

  confirmResetStreak(): void {
    this.tools.pendingConfirm.set(null);
    this.run(() => this.gamification.resetStreak(), (r) => {
      this.tools.applyStreakResult(r.currentStreak);
      return r.message;
    }, true);
  }

  summon(boxType: string): void {
    this.run(
      () => this.gamification.grantLootBox(boxType),
      (box) => {
        this.lootPending.pendingBoxes.update((list) => [box, ...list]);
        return `Summoned ${box.boxType} — pending in Cache.`;
      },
      true
    );
  }

  shiftDay(delta: number): void {
    const next = this.tools.dayOffset() + delta;
    this.run(
      () => this.gamification.setDevClock({ dayOffset: next }),
      (config) => {
        this.tools.applyConfig(config);
        return `Effective today: ${config.effectiveToday} (offset ${config.dayOffset}).`;
      },
      true
    );
  }

  requestClearClock(): void {
    this.tools.pendingConfirm.set('clear-clock');
  }

  confirmClearClock(): void {
    this.tools.pendingConfirm.set(null);
    this.run(
      () => this.gamification.setDevClock({ clear: true }),
      (config) => {
        this.tools.applyConfig(config);
        return `Clock cleared. Effective today: ${config.effectiveToday}.`;
      },
      true
    );
  }

  forceClosePending(): void {
    this.run(
      () => this.gamification.forceClosePending(),
      (r) => {
        this.tools.applyStreakResult(r.streakAfter);
        return (
          `Closed pending days. Streak ${r.streakBefore} → ${r.streakAfter}` +
          (r.alreadyClosed ? ' (already closed).' : '.')
        );
      },
      true
    );
  }

  unsealToday(): void {
    this.run(
      () => this.gamification.unsealToday(),
      (r) => r.message,
      true
    );
  }

  cancelConfirm(): void {
    this.tools.pendingConfirm.set(null);
  }

  private run<T>(
    action: () => import('rxjs').Observable<T>,
    message: (value: T) => string,
    notify = false
  ): void {
    if (this.tools.busy()) {
      return;
    }
    this.tools.busy.set(true);
    action().subscribe({
      next: (value) => {
        this.tools.setFeedback(message(value));
        this.tools.busy.set(false);
        if (notify) {
          this.tools.notifyDataChanged();
        }
      },
      error: (err) => {
        const msg = (err as { error?: { message?: string }; status?: number })?.error?.message;
        const status = (err as { status?: number })?.status;
        this.tools.setFeedback(
          msg ?? (status === 403 ? 'Dev tools disabled on this profile.' : 'Action failed.')
        );
        this.tools.busy.set(false);
      },
    });
  }
}
