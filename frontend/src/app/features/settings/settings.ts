import { Component } from '@angular/core';
import { WorkspacePlaceholder } from '../../shared/workspace-placeholder';
import { workspaces } from '../../core/workspaces';

@Component({
  selector: 'app-settings-page',
  imports: [WorkspacePlaceholder],
  template: '<app-workspace-placeholder [workspace]="workspace" />',
})
export class SettingsPage {
  readonly workspace = workspaces.find(workspace => workspace.path === 'settings')!;
}
