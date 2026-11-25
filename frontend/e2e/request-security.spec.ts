import { expect, test } from '@playwright/test';

test('exchanges the browser CSRF cookie and header for a successful protected request', async ({
  page,
}, testInfo) => {
  await page.goto('/overview');
  const csrfResponsePromise = page.waitForResponse((response) =>
    response.url().endsWith('/api/test-security/csrf'),
  );
  const writeResponsePromise = page.waitForResponse((response) =>
    response.url().endsWith('/api/test-security/write'),
  );
  const writeRequestPromise = page.waitForRequest((request) =>
    request.url().endsWith('/api/test-security/write'),
  );

  if (testInfo.project.name === 'development-proxy') {
    const result = await page.evaluate(async () => {
      const browserWindow = window as Window & {
        runSecurityWrite?: () => Promise<{ result: string }>;
      };
      const body = await browserWindow.runSecurityWrite?.();
      const csrfCookiePresent = document.cookie
        .split('; ')
        .some((value) => value.startsWith('XSRF-TOKEN='));
      return { body: body ?? null, csrfCookiePresent };
    });
    expect(result).toEqual({ body: { result: 'ok' }, csrfCookiePresent: true });
  } else {
    const result = await page.evaluate(async () => {
      await fetch('/api/test-security/csrf', { credentials: 'same-origin' });
      const cookie = document.cookie.split('; ').find((value) => value.startsWith('XSRF-TOKEN='));
      if (!cookie) {
        return { status: 0, csrfCookiePresent: false };
      }
      const token = decodeURIComponent(cookie.slice('XSRF-TOKEN='.length));
      const response = await fetch('/api/test-security/write', {
        method: 'POST',
        credentials: 'same-origin',
        headers: { 'X-XSRF-TOKEN': token },
        body: '{}',
      });
      return { status: response.status, csrfCookiePresent: true };
    });
    expect(result).toEqual({ status: 200, csrfCookiePresent: true });
  }

  const csrfResponse = await csrfResponsePromise;
  const writeResponse = await writeResponsePromise;
  const writeRequest = await writeRequestPromise;

  expect(csrfResponse.status()).toBe(200);
  expect(writeRequest.headers()['x-xsrf-token']).toBeTruthy();
  expect(writeResponse.status()).toBe(200);
});

test('rejects a protected browser request with an invalid CSRF token', async ({ page }) => {
  await page.goto('/overview');
  const status = await page.evaluate(async () => {
    await fetch('/api/test-security/csrf', { credentials: 'same-origin' });
    const response = await fetch('/api/test-security/write', {
      method: 'POST',
      credentials: 'same-origin',
      headers: { 'X-XSRF-TOKEN': 'invalid-test-token' },
      body: '{}',
    });
    return response.status;
  });

  expect(status).toBe(403);
});
