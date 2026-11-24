import { Component } from '@angular/core';
import { WorkspacePlaceholder } from '../../shared/workspace-placeholder';
import { workspaces } from '../../core/workspaces';

@Component({
  selector: 'app-opportunities-page',
  imports: [WorkspacePlaceholder],
  template: '<app-workspace-placeholder [workspace]="workspace" />',
})
export class OpportunitiesPage {
  readonly workspace = workspaces.find(workspace => workspace.path === 'opportunities')!;
}
