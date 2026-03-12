import { registerPlugin } from '@capacitor/core';

import type { CapacitorStatusBarPlugin } from './definitions';

const CapacitorStatusBar = registerPlugin<CapacitorStatusBarPlugin>('CapacitorStatusBar', {
  web: () => import('./web').then((m) => new m.CapacitorStatusBarWeb()),
});

export * from './definitions';
export { CapacitorStatusBar };
