import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { RouterTestingHarness } from '@angular/router/testing';
import { describe, it, expect } from 'vitest';
import { App } from './app';
import { routes } from './app.routes';
import { workspaces } from './core/workspaces';
import { ApplicationContext } from './core/application-context';
import { SourceAttribution } from './shared/source-attribution';

describe('Frontend foundation', () => {
  it('renders the shell and all workspace links', async () => {
    await TestBed.configureTestingModule({ imports: [App], providers: [provideRouter(routes)] }).compileComponents();
    const fixture = TestBed.createComponent(App);
    fixture.detectChanges();
    expect(fixture.nativeElement.textContent).toContain('Profession Companion');
    expect(fixture.nativeElement.querySelectorAll('nav a').length).toBe(8);
  });

  it('navigates to every workspace', async () => {
    TestBed.configureTestingModule({ providers: [provideRouter(routes)] });
    const harness = await RouterTestingHarness.create();
    for (const workspace of workspaces) {
      await harness.navigateByUrl('/' + workspace.path);
      expect(harness.routeNativeElement?.querySelector('h1')?.textContent).toBe(workspace.title);
    }
  });

  it('keeps market context independent of character selection', () => {
    const context = new ApplicationContext();
    expect(context.characterId()).toBeNull();
    context.selectCharacter('test-character');
    expect(context.characterId()).toBe('test-character');
    expect(context.market()).toEqual({ region: 'US', ruleset: 'Normal', faction: 'Alliance' });
    context.selectCharacter(null);
    expect(context.characterId()).toBeNull();
  });

  it('shows a visible ForeverDB link when that source is used', () => {
    const fixture = TestBed.createComponent(SourceAttribution);
    fixture.componentRef.setInput('sources', ['foreverdb']);
    fixture.detectChanges();
    const link = fixture.nativeElement.querySelector('a');
    expect(link.textContent).toBe('ForeverDB.net');
    expect(link.getAttribute('href')).toBe('https://foreverdb.net');
    fixture.componentRef.setInput('sources', []);
    fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('a')).toBeNull();
  });
});
