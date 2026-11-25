import { HttpClient } from '@angular/common/http';
import { bootstrapApplication } from '@angular/platform-browser';
import { firstValueFrom } from 'rxjs';
import { App } from './app/app';
import { appConfig } from './app/app.config';

declare global {
  interface Window {
    runSecurityWrite?: () => Promise<{ result: string }>;
  }
}

void bootstrapApplication(App, appConfig).then((appRef) => {
  window.runSecurityWrite = async () => {
    const client = appRef.injector.get(HttpClient);
    await firstValueFrom(client.get<void>('/api/test-security/csrf'));
    return firstValueFrom(client.post<{ result: string }>('/api/test-security/write', {}));
  };
});
