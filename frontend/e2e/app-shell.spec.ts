import { expect, test } from '@playwright/test';

const workspaces = [
  { path: 'overview', title: 'Overview' },
  { path: 'professions', title: 'Professions' },
  { path: 'market', title: 'Market' },
  { path: 'opportunities', title: 'Opportunities' },
  { path: 'inventory', title: 'Inventory' },
  { path: 'characters', title: 'Characters' },
  { path: 'reference-data', title: 'Reference Data' },
  { path: 'settings', title: 'Settings' },
];

test('renders accessible shell context and disabled placeholder controls', async ({ page }) => {
  await page.goto('/');

  const navigation = page.getByRole('navigation', { name: 'Workspaces' });
  await expect(navigation.getByRole('link')).toHaveCount(workspaces.length);
  await expect(page.getByText('US · Normal · Alliance')).toBeVisible();
  await expect(page.getByRole('button', { name: 'Refresh' })).toBeDisabled();
  await expect(page.getByRole('combobox', { name: 'Character' })).toBeDisabled();
});

test('loads each workspace directly and navigates between workspaces', async ({ page }) => {
  for (const workspace of workspaces) {
    await page.goto(`/${workspace.path}`);
    await expect(page.getByRole('heading', { name: workspace.title, exact: true })).toBeVisible();
  }

  await page.getByRole('link', { name: 'Market' }).click();
  await expect(page).toHaveURL(/\/market$/);
  await expect(page.getByRole('heading', { name: 'Market', exact: true })).toBeVisible();
});
