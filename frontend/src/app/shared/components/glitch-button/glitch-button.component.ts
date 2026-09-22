import { Component, input, output } from '@angular/core';

/**
 * Glitch hover button for primary / danger actions.
 * SuccessHub-themed (gold primary, crimson danger).
 */
@Component({
  selector: 'app-glitch-button',
  standalone: true,
  templateUrl: './glitch-button.component.html',
  styleUrl: './glitch-button.component.scss',
})
export class GlitchButtonComponent {
  readonly label = input.required<string>();
  readonly variant = input<'primary' | 'danger'>('primary');
  readonly disabled = input(false);
  readonly type = input<'button' | 'submit'>('button');
  readonly pressed = output<void>();

  onClick(): void {
    if (this.disabled()) return;
    this.pressed.emit();
  }
}
