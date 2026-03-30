export enum Style {
  LIGHT = 'LIGHT',
  DARK = 'DARK',
  CUSTOM = 'CUSTOM',
}

/**
 * Full HEX color format only (6 or 8 digits).
 * - 6 digits: #RRGGBB (e.g., #FFFFFF, #000000, #FF5733)
 * - 8 digits: #RRGGBBAA with alpha channel (e.g., #FFFFFF00, #FF5733CC)
 *
 * Note: Short 3-digit format (#FFF) is NOT supported.
 */
export type StatusBarColor = `#${string}`;

export enum StatusBarAnimation {
  NONE = 'none',
  FADE = 'fade',
  SLIDE = 'slide',
}

type StatusBarStyleNoDefaultOptions = {
  style: Style;
};

type StatusBarStyleOptions =
  | StatusBarStyleNoDefaultOptions
  | {
      style: Style.CUSTOM;
      color: StatusBarColor;
    };

export type StatusBarOptions = StatusBarStyleOptions;

export type StatusBarSetOverlaysWebViewOptions = {
  value: boolean;
};

export type StatusBarSetBackgroundOptions = {
  color: StatusBarColor;
};

export type SafeAreaInsets = {
  top: number;
  bottom: number;
  left: number;
  right: number;
};

export interface CapacitorStatusBarPlugin {
  /**
   * Set the status bar and navigation bar style and color.
   * @param options - The options to set the status bar style and color.
   * @param options.style - The style of the status bar.
   * @param options.color - The color of the status bar.
   */
  setStyle(options: StatusBarOptions): Promise<void>;
  /**
   * Show the status bar and navigation bar.
   */
  show(): Promise<void>;
  /**
   * Hide the status bar and navigation bar with a slide animation.
   */
  hide(): Promise<void>;
  /**
   * Set whether the status bar overlays the web view.
   *
   * **iOS only** - On Android this is a no-op (resolves without error).
   *
   * - `true`: Web content extends behind the status bar (transparent background),
   *   allowing content to be visible through the status bar area on scroll.
   * - `false`: Restores the status bar background to the color set by `setStyle`
   *   or falls back to the default style from Capacitor config.
   *
   * @param options - The options to set the status bar overlays web view.
   * @param options.value - Whether the status bar overlays the web view (required).
   */
  setOverlaysWebView(options: StatusBarSetOverlaysWebViewOptions): Promise<void>;
  /**
   * Set the window background color.
   * @param options - The options to set the window background color.
   * @param options.color - The background color in HEX format.
   */
  setBackground(options: StatusBarSetBackgroundOptions): Promise<void>;
  /**
   * Show the navigation bar.
   */
  showNavigationBar(): Promise<void>;
  /**
   * Hide the navigation bar with a slide animation (hides completely from screen).
   */
  hideNavigationBar(): Promise<void>;
  /**
   * Get the safe area insets.
   * Returns the insets for status bar, navigation bar, and notch areas.
   * Values are in CSS pixels (dp) on all platforms.
   */
  getSafeAreaInsets(): Promise<SafeAreaInsets>;
}
