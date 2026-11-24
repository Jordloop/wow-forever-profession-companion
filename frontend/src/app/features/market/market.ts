import { Component } from '@angular/core';
import { WorkspacePlaceholder } from '../../shared/workspace-placeholder';
import { workspaces } from '../../core/workspaces';

@Component({
  selector: 'app-market-page',
  imports: [WorkspacePlaceholder],
  template: '<app-workspace-placeholder [workspace]="workspace" />',
})
export class MarketPage {
  readonly workspace = workspaces.find(workspace => workspace.path === 'market')!;
}
