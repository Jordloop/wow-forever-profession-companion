import { Component, input } from '@angular/core';

@Component({
  selector: 'app-workspace-placeholder',
  template: `
    <h1>{{ workspace().title }}</h1>
    <p class="muted">{{ workspace().description }}</p>
    <section class="panel">This workspace is not implemented yet.</section>
  `,
})
export class WorkspacePlaceholder {
  readonly workspace = input.required<{ title: string; description: string }>();
}
