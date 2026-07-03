import { Injectable } from '@angular/core';

type RevealTier = 'default' | 'silver' | 'gold';

/**
 * Lightweight procedural SFX for loot box open / reveal.
 * Uses Web Audio API so no asset files are required.
 */
@Injectable({ providedIn: 'root' })
export class LootAudioService {
  private ctx: AudioContext | null = null;

  playChestOpen(): void {
    const ctx = this.context();
    if (!ctx) return;

    const now = ctx.currentTime;

    const thud = ctx.createOscillator();
    const thudGain = ctx.createGain();
    thud.type = 'triangle';
    thud.frequency.setValueAtTime(140, now);
    thud.frequency.exponentialRampToValueAtTime(55, now + 0.22);
    thudGain.gain.setValueAtTime(0.0001, now);
    thudGain.gain.exponentialRampToValueAtTime(0.22, now + 0.02);
    thudGain.gain.exponentialRampToValueAtTime(0.0001, now + 0.28);
    thud.connect(thudGain);
    thudGain.connect(ctx.destination);
    thud.start(now);
    thud.stop(now + 0.3);

    const latch = ctx.createOscillator();
    const latchGain = ctx.createGain();
    latch.type = 'square';
    latch.frequency.setValueAtTime(880, now + 0.08);
    latch.frequency.exponentialRampToValueAtTime(220, now + 0.16);
    latchGain.gain.setValueAtTime(0.0001, now + 0.08);
    latchGain.gain.exponentialRampToValueAtTime(0.06, now + 0.09);
    latchGain.gain.exponentialRampToValueAtTime(0.0001, now + 0.2);
    latch.connect(latchGain);
    latchGain.connect(ctx.destination);
    latch.start(now + 0.08);
    latch.stop(now + 0.22);

    const creak = ctx.createOscillator();
    const creakGain = ctx.createGain();
    creak.type = 'sawtooth';
    creak.frequency.setValueAtTime(95, now + 0.12);
    creak.frequency.linearRampToValueAtTime(70, now + 0.55);
    creakGain.gain.setValueAtTime(0.0001, now + 0.12);
    creakGain.gain.exponentialRampToValueAtTime(0.04, now + 0.18);
    creakGain.gain.exponentialRampToValueAtTime(0.0001, now + 0.6);
    creak.connect(creakGain);
    creakGain.connect(ctx.destination);
    creak.start(now + 0.12);
    creak.stop(now + 0.62);
  }

  playReveal(tier: RevealTier = 'default'): void {
    const ctx = this.context();
    if (!ctx) return;

    const now = ctx.currentTime;
    const base =
      tier === 'gold' ? 523.25 : tier === 'silver' ? 440 : 349.23;
    const peaks = tier === 'gold' ? [base, base * 1.25, base * 1.5] : [base, base * 1.2];

    peaks.forEach((freq, i) => {
      const osc = ctx.createOscillator();
      const gain = ctx.createGain();
      const start = now + i * 0.07;
      osc.type = 'sine';
      osc.frequency.setValueAtTime(freq, start);
      gain.gain.setValueAtTime(0.0001, start);
      gain.gain.exponentialRampToValueAtTime(tier === 'gold' ? 0.12 : 0.08, start + 0.02);
      gain.gain.exponentialRampToValueAtTime(0.0001, start + 0.35);
      osc.connect(gain);
      gain.connect(ctx.destination);
      osc.start(start);
      osc.stop(start + 0.4);
    });
  }

  /** Crystalline two-note "ting" for flipping a reveal card face-up. */
  playCardFlip(): void {
    const ctx = this.context();
    if (!ctx) return;

    const now = ctx.currentTime;

    // Bright bell-like pair (fifth interval) with a quick shimmer.
    const notes = [
      { freq: 987.77, start: 0, dur: 0.32, gain: 0.09 }, // B5
      { freq: 1479.98, start: 0.045, dur: 0.28, gain: 0.06 }, // F#6
    ];

    notes.forEach(({ freq, start, dur, gain }) => {
      const osc = ctx.createOscillator();
      const g = ctx.createGain();
      osc.type = 'triangle';
      osc.frequency.setValueAtTime(freq, now + start);
      g.gain.setValueAtTime(0.0001, now + start);
      g.gain.exponentialRampToValueAtTime(gain, now + start + 0.012);
      g.gain.exponentialRampToValueAtTime(0.0001, now + start + dur);
      osc.connect(g);
      g.connect(ctx.destination);
      osc.start(now + start);
      osc.stop(now + start + dur + 0.02);
    });
  }

  private context(): AudioContext | null {
    if (typeof window === 'undefined') return null;
    const Ctx = window.AudioContext ?? (window as unknown as { webkitAudioContext?: typeof AudioContext }).webkitAudioContext;
    if (!Ctx) return null;

    if (!this.ctx) {
      this.ctx = new Ctx();
    }
    if (this.ctx.state === 'suspended') {
      void this.ctx.resume();
    }
    return this.ctx;
  }
}
