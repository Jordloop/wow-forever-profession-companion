import { Injectable, signal } from '@angular/core';

@Injectable({ providedIn: 'root' })
export class ApplicationContext {
  readonly market = signal({ region: 'US', ruleset: 'Normal', faction: 'Alliance' }).asReadonly();
  private readonly selectedCharacter = signal<string | null>(null);
  readonly characterId = this.selectedCharacter.asReadonly();

  selectCharacter(id: string | null): void {
    this.selectedCharacter.set(id);
  }
}
