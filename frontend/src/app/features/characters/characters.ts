import { Component } from '@angular/core';
import { WorkspacePlaceholder } from '../../shared/workspace-placeholder';
import { workspaces } from '../../core/workspaces';

@Component({
  selector: 'app-characters-page',
  imports: [WorkspacePlaceholder],
  template: '<app-workspace-placeholder [workspace]="workspace" />',
})
export class CharactersPage {
  readonly workspace = workspaces.find(workspace => workspace.path === 'characters')!;
}
