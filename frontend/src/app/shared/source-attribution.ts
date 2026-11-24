import { Component, input } from '@angular/core';

export type DataSource = 'foreverdb' | 'blizzard' | 'tk';

@Component({
  selector: 'app-source-attribution',
  template: `
    @if (sources().length) {
      <div class="source-attribution">Data sources:
        @for (source of sources(); track source) {
          <a [href]="links[source].url" target="_blank" rel="noopener noreferrer">{{ links[source].name }}</a>
        }
      </div>
    }
  `,
  styles: ['.source-attribution { display: flex; flex-wrap: wrap; gap: 8px; font-size: 12px; color: var(--muted); }'],
})
export class SourceAttribution {
  readonly sources = input.required<readonly DataSource[]>();
  readonly links = {
    foreverdb: { name: 'ForeverDB.net', url: 'https://foreverdb.net' },
    blizzard: { name: 'Blizzard', url: 'https://develop.battle.net/' },
    tk: { name: 'TK Auction House', url: 'https://auctionhouse.tk.gg/' },
  };
}
