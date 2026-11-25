import { Component, inject } from '@angular/core';
import { ErrorNoticeService } from '../core/error-handling';

@Component({
  selector: 'app-error-notice',
  template: `
    @if (notices.notice(); as notice) {
      <section class="error-notice" role="alert">
        <div>
          <p>{{ notice.message }}</p>
          <small>Diagnostic ID: <code>{{ notice.diagnosticId }}</code></small>
        </div>
        <button type="button" aria-label="Dismiss error message" (click)="dismiss()">Dismiss</button>
      </section>
    }
  `,
  styleUrl: './error-notice.scss',
})
export class ErrorNoticeComponent {
  readonly notices = inject(ErrorNoticeService);

  dismiss(): void {
    this.notices.dismiss();
  }
}
