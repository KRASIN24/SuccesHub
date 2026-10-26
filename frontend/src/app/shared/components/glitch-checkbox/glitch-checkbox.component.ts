import { Component, input, model, output } from '@angular/core';

/**
 * Reusable glitch-styled checkbox.
 * Hover glitch overlay always uses the same string as the visible label
 * so the animation width cannot drift short of the text.
 * Pass an empty label + ariaLabel for box-only use (e.g. task rows).
 */
@Component({
  selector: 'app-glitch-checkbox',
  standalone: true,
  templateUrl: './glitch-checkbox.component.html',
  styleUrl: './glitch-checkbox.component.scss',
})
export class GlitchCheckboxComponent {
  /** Visible label text (also drives the hover glitch overlay). Empty = box only. */
  readonly label = input('');
  /** Accessible name when label is empty. */
  readonly ariaLabel = input('');
  readonly disabled = input(false);
  readonly checked = model(false);
  readonly checkedChange = output<boolean>();

  onChange(event: Event): void {
    if (this.disabled()) return;
    const next = (event.target as HTMLInputElement).checked;
    this.checked.set(next);
    this.checkedChange.emit(next);
  }
}
