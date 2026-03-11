import { WebPlugin } from '@capacitor/core';

import type {
  StatusBarOptions,
  StatusBarPlugin,
  StatusBarSetOverlaysWebViewOptions,
  StatusBarShowOptions,
  StatusBarHideOptions,
  StatusBarSetBackgroundOptions,
  SafeAreaInsets,
} from './definitions';

export class StatusBarWeb extends WebPlugin implements StatusBarPlugin {
  async setStyle(options: StatusBarOptions): Promise<void> {
    console.log('setStyle', options);
  }

  async show(options: StatusBarShowOptions): Promise<void> {
    console.log('show', options);
  }

  async hide(options: StatusBarHideOptions): Promise<void> {
    console.log('hide', options);
  }

  async setOverlaysWebView(options: StatusBarSetOverlaysWebViewOptions): Promise<void> {
    console.log('setOverlaysWebView', options);
  }

  async setBackground(options: StatusBarSetBackgroundOptions): Promise<void> {
    console.log('setBackground', options);
  }

  async getSafeAreaInsets(): Promise<SafeAreaInsets> {
    // On web, we can use CSS environment variables to get safe area insets
    // These are set by the browser on devices with notches, etc.
    const getInsetValue = (variable: string): number => {
      const value = getComputedStyle(document.documentElement).getPropertyValue(variable).trim();
      return value ? parseInt(value, 10) : 0;
    };

    const insets: SafeAreaInsets = {
      top: getInsetValue('env(safe-area-inset-top)') || getInsetValue('constant(safe-area-inset-top)') || 0,
      bottom: getInsetValue('env(safe-area-inset-bottom)') || getInsetValue('constant(safe-area-inset-bottom)') || 0,
      left: getInsetValue('env(safe-area-inset-left)') || getInsetValue('constant(safe-area-inset-left)') || 0,
      right: getInsetValue('env(safe-area-inset-right)') || getInsetValue('constant(safe-area-inset-right)') || 0,
    };

    console.log('getSafeAreaInsets', insets);
    return insets;
  }
}
