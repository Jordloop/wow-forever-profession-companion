import { ErrorHandler, Injectable, inject, signal } from '@angular/core';
import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { catchError, throwError } from 'rxjs';

export interface ErrorNotice {
  readonly message: string;
  readonly diagnosticId: string;
}

@Injectable({ providedIn: 'root' })
export class ErrorNoticeService {
  private readonly currentNotice = signal<ErrorNotice | null>(null);
  readonly notice = this.currentNotice.asReadonly();

  reportHttpFailure(error: HttpErrorResponse): void {
    this.currentNotice.set({
      message: messageForStatus(error.status),
      diagnosticId: diagnosticIdFor(error),
    });
  }

  reportUnexpectedFailure(error: unknown): void {
    const diagnosticId = crypto.randomUUID();
    const category = errorCategory(error);
    console.error(`Unhandled application error (${diagnosticId}): ${category}`);
    this.currentNotice.set({
      message: 'An unexpected application error occurred. Provide this diagnostic ID if the problem continues.',
      diagnosticId,
    });
  }

  dismiss(): void {
    this.currentNotice.set(null);
  }
}

export const apiErrorInterceptor: HttpInterceptorFn = (_request, next) => {
  const notices = inject(ErrorNoticeService);
  return next(_request).pipe(
    catchError((error: unknown) => {
      if (error instanceof HttpErrorResponse) {
        notices.reportHttpFailure(error);
      }
      return throwError(() => error);
    }),
  );
};

@Injectable()
export class ApplicationErrorHandler extends ErrorHandler {
  private readonly notices = inject(ErrorNoticeService);

  override handleError(error: unknown): void {
    this.notices.reportUnexpectedFailure(error);
  }
}

function messageForStatus(status: number): string {
  if (status === 0) {
    return 'The server could not be reached. Check the connection and try again.';
  }
  if (status === 400 || status === 422) {
    return 'Some information was not accepted. Review it and try again.';
  }
  if (status === 404) {
    return 'The requested resource was not found. It may have been removed.';
  }
  if (status >= 500) {
    return 'A server error occurred. Try again. Provide this diagnostic ID if the problem continues.';
  }
  return 'The request could not be completed. Check the information and try again.';
}

function diagnosticIdFor(error: HttpErrorResponse): string {
  const headerId = error.headers.get('X-Correlation-ID');
  if (isCorrelationId(headerId)) {
    return headerId;
  }
  const bodyId = correlationIdFrom(error.error);
  return isCorrelationId(bodyId) ? bodyId : crypto.randomUUID();
}

function isCorrelationId(value: string | null): value is string {
  return value !== null && /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i.test(value);
}

function correlationIdFrom(body: unknown): string | null {
  if (typeof body === 'object' && body !== null && 'correlationId' in body) {
    const value = body.correlationId;
    return typeof value === 'string' ? value : null;
  }
  return null;
}

function errorCategory(error: unknown): string {
  if (error instanceof TypeError) {
    return 'TypeError';
  }
  if (error instanceof RangeError) {
    return 'RangeError';
  }
  return error instanceof Error ? 'Error' : 'Unknown';
}
