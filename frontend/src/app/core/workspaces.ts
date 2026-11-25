export const workspaces = [
  { path: 'overview', title: 'Overview', description: 'Character status and next decisions', group: 'Gameplay' },
  { path: 'professions', title: 'Professions', description: 'Select a milestone, then compare strategies', group: 'Gameplay' },
  { path: 'market', title: 'Market', description: 'Browse supported items independently of profession', group: 'Gameplay' },
  { path: 'opportunities', title: 'Opportunities', description: 'Compare processing, gathering and unusual prices', group: 'Gameplay' },
  { path: 'inventory', title: 'Inventory', description: 'Recorded character holdings', group: 'Gameplay' },
  { path: 'characters', title: 'Characters', description: 'Separate skills, inventory and goals', group: 'Management' },
  { path: 'reference-data', title: 'Reference Data', description: 'Missing or conflicting fields and their effects', group: 'Management' },
  { path: 'settings', title: 'Settings', description: 'Synchronization and local preferences', group: 'Management' },
] as const;
