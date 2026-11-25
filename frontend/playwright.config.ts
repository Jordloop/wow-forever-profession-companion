import { defineConfig } from '@playwright/test';

export default defineConfig({
  testDir: './e2e',
  fullyParallel: true,
  reporter: 'list',
  globalSetup: './e2e/global-setup.ts',
  use: {
    browserName: 'chromium',
    headless: true,
    trace: 'retain-on-failure',
  },
  projects: [
    {
      name: 'production-compose',
      use: { baseURL: 'http://127.0.0.1:4320' },
    },
    {
      name: 'development-proxy',
      use: { baseURL: 'http://127.0.0.1:4321' },
    },
  ],
  webServer: {
    command:
      'npm run start -- --configuration e2e --host 127.0.0.1 --port 4321 --proxy-config proxy.e2e.conf.json',
    url: 'http://127.0.0.1:4321',
    reuseExistingServer: false,
    timeout: 120_000,
  },
});
