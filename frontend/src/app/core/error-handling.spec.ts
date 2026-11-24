import { HttpClient, HttpErrorResponse, provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { afterEach, describe, expect, it, vi } from 'vitest';
import { ApplicationErrorHandler, apiErrorInterceptor, ErrorNoticeService } from './error-handling';
import { ErrorNoticeComponent } from '../shared/error-notice';

describe('Application error handling', () => {
  afterEach(() => {
    TestBed.resetTestingModule();
    vi.restoreAllMocks();
  });

  it('shows a safe message and the server correlation ID for HTTP failures', () => {
    TestBed.configureTestingModule({
      imports: [ErrorNoticeComponent],
      providers: [
        provideHttpClient(withInterceptors([apiErrorInterceptor])),
        provideHttpClientTesting(),
      ],
    });
    const client = TestBed.inject(HttpClient);
    const requests = TestBed.inject(HttpTestingController);
    const notices = TestBed.inject(ErrorNoticeService);
    let receivedError: unknown;

    client.get('/api/example').subscribe({ error: error => receivedError = error });
    requests.expectOne('/api/example').flush(
      { detail: 'private server detail', correlationId: '01234567-89ab-cdef-0123-456789abcdef' },
      {
        status: 500,
        statusText: 'Internal Server Error',
        headers: { 'X-Correlation-ID': '01234567-89ab-cdef-0123-456789abcdef' },
      },
    );

    expect(receivedError).toBeInstanceOf(HttpErrorResponse);
    expect(notices.notice()).toEqual({
      message: 'A server error occurred. Try again. Provide this diagnostic ID if the problem continues.',
      diagnosticId: '01234567-89ab-cdef-0123-456789abcdef',
    });

    const fixture = TestBed.createComponent(ErrorNoticeComponent);
    fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('[role="alert"]').textContent).toContain(
      '01234567-89ab-cdef-0123-456789abcdef',
    );
    fixture.nativeElement.querySelector('button').click();
    fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('[role="alert"]')).toBeNull();
    requests.verify();
  });

  it('generates a diagnostic ID for connection failures', () => {
    const notices = new ErrorNoticeService();
    notices.reportHttpFailure(new HttpErrorResponse({ status: 0 }));

    expect(notices.notice()?.message).toContain('server could not be reached');
    expect(notices.notice()?.diagnosticId).toMatch(
      /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i,
    );
  });

  it('does not log or display uncaught exception details', () => {
    TestBed.configureTestingModule({ providers: [ApplicationErrorHandler] });
    const notices = TestBed.inject(ErrorNoticeService);
    const handler = TestBed.inject(ApplicationErrorHandler);
    const log = vi.spyOn(console, 'error').mockImplementation(() => undefined);

    handler.handleError(new Error('private client detail'));

    expect(log).toHaveBeenCalledTimes(1);
    expect(log.mock.calls[0].join(' ')).not.toContain('private client detail');
    expect(notices.notice()?.message).toContain('unexpected application error');
    expect(notices.notice()?.diagnosticId).toMatch(
      /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i,
    );
  });
});
