import {
  ChangeDetectionStrategy,
  Component,
  OnInit,
  computed,
  effect,
  inject,
  input,
  output,
  signal,
  untracked,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { DatePickerModule } from 'primeng/datepicker';
import { Observable } from 'rxjs';
import { GamificationService } from '../../../core/services/gamification.service';
import { DevToolsService } from '../../../core/services/dev-tools.service';
import {
  InventoryItem,
  StreakActionResult,
  StreakCalendar,
  StreakDay,
  StreakDayStatus,
} from '../../../core/models/gamification.model';
import { DevToolsChipComponent } from '../../../shared/components/dev-tools/dev-tools-chip.component';

/** Shape of the date object PrimeNG passes to the `date` template. `month` is 0-indexed. */
interface DateCell {
  day: number;
  month: number;
  year: number;
  otherMonth?: boolean;
  today?: boolean;
  selectable?: boolean;
}

/**
 * Streak management panel: calendar heat-map, shield/card usage.
 * Manual streak testing tools live in the Dev bubble system when enabled.
 */
@Component({
  selector: 'app-streak-panel',
  standalone: true,
  imports: [CommonModule, FormsModule, DatePickerModule, DevToolsChipComponent],
  templateUrl: './streak-panel.component.html',
  styleUrl: './streak-panel.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class StreakPanelComponent implements OnInit {
  private readonly gamification = inject(GamificationService);
  private readonly devTools = inject(DevToolsService);

  /** Utility inventory items (shields + XP boosts) usable from this panel. */
  readonly cards = input<InventoryItem[]>([]);
  /** Emitted after any mutation so the parent can refresh profile + inventory. */
  readonly changed = output<void>();

  readonly calendar = signal<StreakCalendar | null>(null);
  readonly loading = signal(true);
  readonly error = signal<string | null>(null);
  readonly busy = signal(false);
  readonly feedback = signal<string | null>(null);

  readonly viewDate = signal<Date>(new Date());
  readonly selectedDate = signal<Date | null>(null);

  private readonly curYear = signal<number>(new Date().getFullYear());
  private readonly curMonth = signal<number>(new Date().getMonth() + 1);

  private readonly dayMap = computed(() => {
    const map = new Map<string, StreakDay>();
    for (const d of this.calendar()?.days ?? []) {
      map.set(d.date, d);
    }
    return map;
  });

  readonly shieldCards = computed(() => this.cards().filter((c) => c.reward.type === 'SHIELD'));
  readonly boostCards = computed(() => this.cards().filter((c) => c.reward.type === 'XP_BOOST'));

  readonly selectedKey = computed(() => {
    const d = this.selectedDate();
    return d ? this.iso(d.getFullYear(), d.getMonth(), d.getDate()) : null;
  });
  readonly selectedStatus = computed<StreakDayStatus | null>(() => {
    const key = this.selectedKey();
    return key ? this.dayMap().get(key)?.status ?? null : null;
  });
  readonly canShieldSelected = computed(
    () => this.selectedStatus() === 'MISSED' && (this.calendar()?.shieldsAvailable ?? 0) > 0
  );

  constructor() {
    // Reload when Dev tools mutate streak / clock / ritual state.
    effect(() => {
      const rev = this.devTools.dataRevision();
      untracked(() => {
        if (rev === 0) {
          return;
        }
        this.reloadCurrent();
        this.changed.emit();
      });
    });
  }

  ngOnInit(): void {
    this.load(this.curYear(), this.curMonth());
  }

  load(year: number, month: number): void {
    this.curYear.set(year);
    this.curMonth.set(month);
    // Keep the datepicker mounted on month switches — destroying it reset the view to
    // "today" while dayMap still held the fetched month, so colors looked wrong.
    const initial = this.calendar() == null;
    if (initial) {
      this.loading.set(true);
    }
    this.error.set(null);
    const key = `${year}-${String(month).padStart(2, '0')}`;
    this.gamification.getStreakCalendar(key).subscribe({
      next: (cal) => {
        this.calendar.set(cal);
        this.devTools.applyStreakResult(cal.currentStreak);
        this.loading.set(false);
      },
      error: () => {
        this.error.set('Could not load your streak calendar.');
        this.loading.set(false);
      },
    });
  }

  onMonthChange(event: { month?: number; year?: number }): void {
    if (event.month == null || event.year == null) {
      return;
    }
    this.selectedDate.set(null);
    // PrimeNG emits 1-based month; keep viewDate in sync for remounts / defaultDate.
    this.viewDate.set(new Date(event.year, event.month - 1, 1));
    this.load(event.year, event.month);
  }

  onSelect(date: Date): void {
    this.selectedDate.set(date);
    this.feedback.set(null);
  }

  dayClass(cell: DateCell): Record<string, boolean> {
    const status = cell.otherMonth
      ? null
      : this.dayMap().get(this.iso(cell.year, cell.month, cell.day))?.status ?? null;
    return {
      'streak-day': true,
      'streak-day--other': !!cell.otherMonth,
      'streak-day--completed': status === 'COMPLETED',
      'streak-day--missed': status === 'MISSED',
      'streak-day--shielded': status === 'SHIELDED',
      'streak-day--today': status === 'TODAY',
      'streak-day--future': status === 'FUTURE' || status === 'EMPTY',
    };
  }

  protectSelectedDay(): void {
    const key = this.selectedKey();
    if (!key || this.busy()) {
      return;
    }
    this.run(this.gamification.shieldDay(key));
  }

  useCard(item: InventoryItem): void {
    if (this.busy()) {
      return;
    }
    this.busy.set(true);
    this.gamification.useInventoryItem(item.id).subscribe({
      next: (result) => {
        this.feedback.set(result.message);
        this.busy.set(false);
        this.reloadCurrent();
        this.changed.emit();
      },
      error: (err) => {
        this.feedback.set(this.errMessage(err));
        this.busy.set(false);
      },
    });
  }

  private run(action: Observable<StreakActionResult>): void {
    this.busy.set(true);
    action.subscribe({
      next: (result) => {
        this.feedback.set(result.message);
        this.busy.set(false);
        this.selectedDate.set(null);
        this.reloadCurrent();
        this.changed.emit();
      },
      error: (err) => {
        this.feedback.set(this.errMessage(err));
        this.busy.set(false);
      },
    });
  }

  retry(): void {
    this.reloadCurrent();
  }

  private reloadCurrent(): void {
    this.load(this.curYear(), this.curMonth());
  }

  private errMessage(err: unknown): string {
    const message = (err as { error?: { message?: string } })?.error?.message;
    return message ?? 'Something went wrong. Please try again.';
  }

  private iso(year: number, month0: number, day: number): string {
    return `${year}-${String(month0 + 1).padStart(2, '0')}-${String(day).padStart(2, '0')}`;
  }
}
