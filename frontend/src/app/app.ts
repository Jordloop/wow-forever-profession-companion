import { Component, inject } from '@angular/core';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { ApplicationContext } from './core/application-context';
import { workspaces } from './core/workspaces';

@Component({
  selector: 'app-root',
  imports: [RouterLink, RouterLinkActive, RouterOutlet],
  templateUrl: './app.html',
  styleUrl: './app.scss',
})
export class App {
  readonly context = inject(ApplicationContext);
  readonly groups = ['Gameplay', 'Management'] as const;
  readonly workspaces = workspaces;
}
