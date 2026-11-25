import { Routes } from '@angular/router';

export const routes: Routes = [
  { path: '', pathMatch: 'full', redirectTo: 'overview' },
  { path: 'overview', loadComponent: () => import('./features/overview/overview').then(m => m.OverviewPage) },
  { path: 'professions', loadComponent: () => import('./features/professions/professions').then(m => m.ProfessionsPage) },
  { path: 'market', loadComponent: () => import('./features/market/market').then(m => m.MarketPage) },
  { path: 'opportunities', loadComponent: () => import('./features/opportunities/opportunities').then(m => m.OpportunitiesPage) },
  { path: 'inventory', loadComponent: () => import('./features/inventory/inventory').then(m => m.InventoryPage) },
  { path: 'characters', loadComponent: () => import('./features/characters/characters').then(m => m.CharactersPage) },
  { path: 'reference-data', loadComponent: () => import('./features/reference-data/reference-data').then(m => m.ReferenceDataPage) },
  { path: 'settings', loadComponent: () => import('./features/settings/settings').then(m => m.SettingsPage) },
  { path: '**', redirectTo: 'overview' },
];
