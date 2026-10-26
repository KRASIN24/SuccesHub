import { Injectable, signal } from '@angular/core';
import { RECOMMENDED_PICKS } from './visual-lab-specimens';

const STORAGE_KEY = 'successhub_visual_lab_picks';

/**
 * Persists Visual Lab winner picks in localStorage for W15 handoff.
 */
@Injectable({ providedIn: 'root' })
export class VisualLabPicksService {
  readonly picks = signal<Record<string, string>>(this.load());

  setPick(groupId: string, specimenId: string): void {
    this.picks.update((current) => {
      const next = { ...current, [groupId]: specimenId };
      this.persist(next);
      return next;
    });
  }

  clear(): void {
    this.picks.set({ ...RECOMMENDED_PICKS });
    this.persist(this.picks());
  }

  resetToEmpty(): void {
    this.picks.set({});
    localStorage.removeItem(STORAGE_KEY);
  }

  exportJson(): string {
    return JSON.stringify(
      {
        version: 1,
        exportedAt: new Date().toISOString(),
        picks: this.picks(),
      },
      null,
      2
    );
  }

  private load(): Record<string, string> {
    try {
      const raw = localStorage.getItem(STORAGE_KEY);
      if (!raw) {
        return { ...RECOMMENDED_PICKS };
      }
      const parsed = JSON.parse(raw) as unknown;
      if (
        parsed &&
        typeof parsed === 'object' &&
        'picks' in parsed &&
        (parsed as { picks: unknown }).picks &&
        typeof (parsed as { picks: unknown }).picks === 'object'
      ) {
        return {
          ...RECOMMENDED_PICKS,
          ...(parsed as { picks: Record<string, string> }).picks,
        };
      }
      if (parsed && typeof parsed === 'object') {
        return { ...RECOMMENDED_PICKS, ...(parsed as Record<string, string>) };
      }
      return { ...RECOMMENDED_PICKS };
    } catch {
      return { ...RECOMMENDED_PICKS };
    }
  }

  private persist(picks: Record<string, string>): void {
    localStorage.setItem(STORAGE_KEY, JSON.stringify({ version: 1, picks }));
  }
}
