import { HttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { afterEach, describe, expect, it } from 'vitest';
import { appConfig } from '../app.config';

describe('Angular CSRF support', () => {
  afterEach(() => {
    document.cookie = 'XSRF-TOKEN=; Max-Age=0; Path=/';
    TestBed.resetTestingModule();
  });

  it('sends the CSRF cookie as the expected header on relative API writes', () => {
    TestBed.configureTestingModule({
      providers: [...appConfig.providers, provideHttpClientTesting()],
    });
    document.cookie = 'XSRF-TOKEN=test-csrf-token; Path=/';
    const client = TestBed.inject(HttpClient);
    const requests = TestBed.inject(HttpTestingController);

    client.post('/api/test-security/write', {}).subscribe();
    const request = requests.expectOne('/api/test-security/write');

    expect(request.request.headers.get('X-XSRF-TOKEN')).toBe('test-csrf-token');
    request.flush(null);
    requests.verify();
  });
});
