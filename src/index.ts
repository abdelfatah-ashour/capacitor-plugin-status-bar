import { registerPlugin } from '@capacitor/core';

import type { StatusBarPlugin } from './definitions';

const StatusBar = registerPlugin<StatusBarPlugin>('CapacitorStatusBar', {
  web: () => import('./web').then((m) => new m.StatusBarWeb()),
});

export * from './definitions';
export { StatusBar };
