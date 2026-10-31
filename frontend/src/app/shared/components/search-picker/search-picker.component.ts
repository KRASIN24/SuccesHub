import {
  Component,
  ElementRef,
  HostListener,
  computed,
  inject,
  input,
  output,
  signal,
} from '@angular/core';
import { FormsModule } from '@angular/forms';
import { TranslatePipe } from '../../../core/i18n/translate.pipe';
import { GlitchCheckboxComponent } from '../glitch-checkbox/glitch-checkbox.component';

export interface SearchPickerOption {
  id: string;
  label: string;
}

/**
 * Searchable droplist for dark UI.
 * - single: filter + click row to pick
 * - multiple: filter + checkboxes + confirm button
 */
@Component({
  selector: 'app-search-picker',
  standalone: true,
  imports: [FormsModule, TranslatePipe, GlitchCheckboxComponent],
  templateUrl: './search-picker.component.html',
  styleUrl: './search-picker.component.scss',
})
export class SearchPickerComponent {
  private readonly host = inject(ElementRef<HTMLElement>);

  readonly options = input<SearchPickerOption[]>([]);
  readonly multiple = input(false);
  readonly placeholder = input('Search…');
  readonly emptyLabel = input('Nothing to pick');
  readonly confirmLabel = input('Confirm');
  readonly disabled = input(false);

  readonly picked = output<string>();
  readonly confirmed = output<string[]>();

  readonly open = signal(false);
  readonly query = signal('');
  readonly checkedIds = signal<Set<string>>(new Set());

  readonly filtered = computed(() => {
    const q = this.query().trim().toLowerCase();
    const opts = this.options();
    if (!q) return opts;
    return opts.filter((o) => o.label.toLowerCase().includes(q));
  });

  readonly checkedCount = computed(() => this.checkedIds().size);

  toggleOpen(): void {
    if (this.disabled() || this.options().length === 0) return;
    this.open.update((v) => !v);
    if (!this.open()) {
      this.query.set('');
    }
  }

  selectOne(id: string): void {
    this.picked.emit(id);
    this.open.set(false);
    this.query.set('');
  }

  toggleCheck(id: string, checked: boolean): void {
    const next = new Set(this.checkedIds());
    if (checked) next.add(id);
    else next.delete(id);
    this.checkedIds.set(next);
  }

  isChecked(id: string): boolean {
    return this.checkedIds().has(id);
  }

  confirm(): void {
    const ids = [...this.checkedIds()];
    if (ids.length === 0) return;
    this.confirmed.emit(ids);
    this.checkedIds.set(new Set());
    this.open.set(false);
    this.query.set('');
  }

  @HostListener('document:click', ['$event'])
  onDocClick(event: MouseEvent): void {
    if (!this.open()) return;
    if (!this.host.nativeElement.contains(event.target as Node)) {
      this.open.set(false);
      this.query.set('');
    }
  }
}
