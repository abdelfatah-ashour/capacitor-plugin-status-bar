package com.cap.plugins.statusbar;

import android.app.Activity;
import android.graphics.Color;
import android.os.Build;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.widget.FrameLayout;
import android.view.Gravity;

import androidx.annotation.ColorInt;
import androidx.annotation.Nullable;
import androidx.core.graphics.ColorUtils;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;

import com.getcapacitor.Plugin;


/**
 * Android Status Bar utilities with Android 10-15+ (API 29-35+) support.
 * Supports:
 * - API 29 (Android 10): Uses deprecated SYSTEM_UI_FLAG for backward
 * compatibility
 * - API 30+ (Android 11+): Uses modern WindowInsetsController API
 * - API 35+ (Android 15+): Fully compatible with edge-to-edge display
 * enforcement
 */
public class StatusBar extends Plugin {
    private static final String TAG = "StatusBar";
    private static final String STATUS_BAR_OVERLAY_TAG = "capacitor_status_bar_overlay";
    private static final String NAV_BAR_OVERLAY_TAG = "capacitor_navigation_bar_overlay";

    // Store current state to preserve colors when hiding/showing
    private String currentStyle = "LIGHT";
    private String currentColorHex = null;
    private int currentStatusBarColor = Color.BLACK;
    private int currentNavBarColor = Color.BLACK;

    // Note: load() is not called since StatusBar is instantiated via new StatusBar()
    // from StatusBarPlugin, not registered as a Capacitor plugin itself.
    // All initialization happens via ensureEdgeToEdgeConfigured() called from StatusBarPlugin.load().

    /**
     * Ensures edge-to-edge is properly configured for Android 15+.
     * Sets up a unified insets listener on the decorView that handles both
     * IME insets and overlay view sizing.
     *
     * @param activity The activity to configure
     */
    public void ensureEdgeToEdgeConfigured(Activity activity) {
        Window window = activity.getWindow();
        View decorView = window.getDecorView();

        // Enable edge-to-edge for ALL API levels.
        // window.setStatusBarColor() is unreliable on many devices/OEMs,
        // so we use overlay views to control bar colors consistently.
        WindowCompat.setDecorFitsSystemWindows(window, false);

        // Make native bar colors transparent so our overlays are the sole color source
        window.setStatusBarColor(Color.TRANSPARENT);
        window.setNavigationBarColor(Color.TRANSPARENT);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.setStatusBarContrastEnforced(false);
            window.setNavigationBarContrastEnforced(false);
        }

        // Single unified insets listener on decorView for overlay sizing
        ViewCompat.setOnApplyWindowInsetsListener(decorView, (v, insets) -> {
            // Size status bar overlay
            int top = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top;
            View statusOverlay = ((ViewGroup) v).findViewWithTag(STATUS_BAR_OVERLAY_TAG);
            if (statusOverlay != null) {
                ViewGroup.LayoutParams params = statusOverlay.getLayoutParams();
                if (params.height != top) {
                    params.height = top;
                    statusOverlay.setLayoutParams(params);
                    Log.d(TAG, "insetsListener: status bar overlay height=" + top);
                }
            }

            // Size navigation bar overlay
            int bottom = insets.getInsets(WindowInsetsCompat.Type.navigationBars()).bottom;
            View navOverlay = ((ViewGroup) v).findViewWithTag(NAV_BAR_OVERLAY_TAG);
            if (navOverlay != null) {
                ViewGroup.LayoutParams params = navOverlay.getLayoutParams();
                if (params.height != bottom) {
                    params.height = bottom;
                    navOverlay.setLayoutParams(params);
                    Log.d(TAG, "insetsListener: nav bar overlay height=" + bottom);
                }
            }

            ViewCompat.onApplyWindowInsets(v, insets);
            return insets;
        });

        Log.d(TAG, "ensureEdgeToEdgeConfigured: edge-to-edge with overlay views, API=" + Build.VERSION.SDK_INT);
    }

    public void setOverlaysWebView(Activity activity, boolean overlay) {
        // No-op on Android. Exposed for API parity with iOS.
        Log.d(TAG, "setOverlaysWebView: no-op on Android, overlay=" + overlay);
    }

    public void showStatusBar(Activity activity, boolean animated) {
        Log.d(TAG, "showStatusBar: animated=" + animated + ", currentStyle=" + currentStyle + ", API="
                + Build.VERSION.SDK_INT);
        Window window = activity.getWindow();
        View decorView = window.getDecorView();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // API 30+ (Android 11+) - Use WindowInsetsController
            WindowInsetsController controller = window.getInsetsController();
            if (controller != null) {
                Log.d(TAG, "showStatusBar: showing system bars (API 30+)");
                // Show both status and navigation bars together
                controller.show(WindowInsets.Type.systemBars());
                // Set behavior for transient bars (user can swipe to reveal)
                controller.setSystemBarsBehavior(
                        WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
            } else {
                Log.w(TAG, "showStatusBar: WindowInsetsController is null");
            }
        } else {
            // API 29 (Android 10) - Use system UI visibility flags (deprecated but
            // necessary)
            Log.d(TAG, "showStatusBar: showing using system UI flags (API 29)");
            // Set to visible state - clear all immersive flags
            decorView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_VISIBLE);
        }

        // Reapply the stored colors and style instead of removing them
        reapplyCurrentStyle(activity);

        // Restore the overlay backgrounds to their original colors
        restoreStatusBarBackground(activity);
    }

    public void hideStatusBar(Activity activity, String animation) {
        Log.d(TAG, "hideStatusBar: animation=" + animation + ", API=" + Build.VERSION.SDK_INT);
        Window window = activity.getWindow();
        View decorView = window.getDecorView();

        String animationType = animation != null ? animation.toLowerCase() : "slide";

        if ("fade".equals(animationType)) {
            // Fade mode: Make background transparent without removing status bar and
            // navigation bar
            Log.d(TAG, "hideStatusBar: fade mode - making backgrounds transparent");
            makeStatusBarBackgroundTransparent(activity);
        } else if ("slide".equals(animationType)) {
            // Slide mode: Hide status bar and navigation bar completely (current behavior)
            Log.d(TAG, "hideStatusBar: slide mode - hiding bars completely");

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                // API 30+ (Android 11+) - Use WindowInsetsController
                WindowInsetsController controller = window.getInsetsController();
                if (controller != null) {
                    Log.d(TAG, "hideStatusBar: hiding system bars (API 30+)");
                    // Hide both status and navigation bars together
                    controller.hide(WindowInsets.Type.systemBars());
                    // Set behavior for immersive mode (user can swipe to reveal temporarily)
                    controller.setSystemBarsBehavior(
                            WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
                } else {
                    Log.w(TAG, "hideStatusBar: WindowInsetsController is null");
                }
            } else {
                // API 29 (Android 10) - Use system UI visibility flags (deprecated but
                // necessary)
                Log.d(TAG, "hideStatusBar: hiding using system UI flags (API 29)");
                // Use immersive sticky mode with proper layout flags for Android 10
                decorView.setSystemUiVisibility(
                        View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                                | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                                | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                                | View.SYSTEM_UI_FLAG_FULLSCREEN);
            }

            // Make the overlay backgrounds transparent so content shows through
            makeStatusBarBackgroundTransparent(activity);
        } else {
            // Unknown animation type, default to slide
            Log.w(TAG, "hideStatusBar: unknown animation type '" + animationType + "', defaulting to slide");
            hideStatusBar(activity, "slide");
        }
    }

    public void setStyle(Activity activity, String style, @Nullable String colorHex) {
        Log.d(TAG, "setStyle: style=" + style + ", colorHex=" + colorHex);
        Window window = activity.getWindow();

        // Store the current style and color for later reapplication
        currentStyle = style;
        currentColorHex = colorHex;

        // Set icon appearance (light/dark) regardless of background approach
        boolean lightBackground;
        if ("LIGHT".equalsIgnoreCase(style)) {
            // Light background -> dark icons
            setLightStatusBarIcons(window, true);
            lightBackground = true;
        } else if ("DARK".equalsIgnoreCase(style)) {
            // Dark background -> light icons
            setLightStatusBarIcons(window, false);
            lightBackground = false;
        } else if ("CUSTOM".equalsIgnoreCase(style)) {
            // CUSTOM: Derive icon color from provided custom color
            int parsed = parseColorOrDefault(colorHex, Color.BLACK);
            boolean isLight = isEffectiveLightColor(parsed);
            // If background is light, request dark icons
            setLightStatusBarIcons(window, isLight);
            lightBackground = isLight;
        } else {
            // Default: Auto-detect based on system theme (follow device theme)
            boolean isSystemDarkMode = isSystemInDarkMode(activity);
            setLightStatusBarIcons(window, !isSystemDarkMode);
            lightBackground = !isSystemDarkMode;
        }

        if ("CUSTOM".equalsIgnoreCase(style) && colorHex != null) {
            int color = parseColorOrDefault(colorHex, lightBackground ? Color.WHITE : Color.BLACK);
            currentStatusBarColor = color;
            currentNavBarColor = color;
            applyStatusBarBackground(activity, color);
            applyNavigationBarBackground(activity, color);
        } else if ("LIGHT".equalsIgnoreCase(style)) {
            currentStatusBarColor = Color.WHITE;
            currentNavBarColor = Color.WHITE;
            applyStatusBarBackground(activity, Color.WHITE);
            applyNavigationBarBackground(activity, Color.WHITE);
        } else if ("DARK".equalsIgnoreCase(style)) {
            currentStatusBarColor = Color.BLACK;
            currentNavBarColor = Color.BLACK;
            applyStatusBarBackground(activity, Color.BLACK);
            applyNavigationBarBackground(activity, Color.BLACK);
        } else {
            // Default: Auto-detect based on system theme
            boolean isSystemDarkMode = isSystemInDarkMode(activity);
            int themeColor = isSystemDarkMode ? Color.BLACK : Color.WHITE;
            currentStatusBarColor = themeColor;
            currentNavBarColor = themeColor;
            applyStatusBarBackground(activity, themeColor);
            applyNavigationBarBackground(activity, themeColor);
        }
    }

    /**
     * Set the window background color.
     *
     * @param activity The activity to apply the background color to
     * @param colorHex The hex color string (e.g., "#FFFFFF" or "#FF5733")
     */
    public void setBackground(Activity activity, @Nullable String colorHex) {
        Log.d(TAG, "setBackground: colorHex=" + colorHex);

        if (colorHex == null) {
            Log.w(TAG, "setBackground: colorHex is null");
            return;
        }

        int color = parseColorOrDefault(colorHex, Color.WHITE);
        applyWindowBackground(activity, color);
    }

    /**
     * Get the safe area insets.
     * Returns the insets for status bar, navigation bar, and notch areas.
     *
     * @param activity The activity to get the insets from
     * @return A map containing top, bottom, left, and right inset values in dp (density-independent pixels)
     */
    public java.util.Map<String, Integer> getSafeAreaInsets(Activity activity) {
        Log.d(TAG, "getSafeAreaInsets");
        java.util.Map<String, Integer> insets = new java.util.HashMap<>();
        float density = activity.getResources().getDisplayMetrics().density;

        View decorView = activity.getWindow().getDecorView();
        WindowInsetsCompat windowInsets = ViewCompat.getRootWindowInsets(decorView);

        if (windowInsets != null) {
            // Use WindowInsetsCompat for accurate, type-specific insets across all API levels
            androidx.core.graphics.Insets statusBars = windowInsets.getInsets(WindowInsetsCompat.Type.statusBars());
            androidx.core.graphics.Insets navBars = windowInsets.getInsets(WindowInsetsCompat.Type.navigationBars());
            androidx.core.graphics.Insets displayCutout = windowInsets.getInsets(WindowInsetsCompat.Type.displayCutout());

            int topPx = Math.max(statusBars.top, displayCutout.top);
            int bottomPx = Math.max(navBars.bottom, displayCutout.bottom);
            int leftPx = Math.max(navBars.left, displayCutout.left);
            int rightPx = Math.max(navBars.right, displayCutout.right);

            // Convert physical pixels to dp (CSS pixels) for the WebView layer
            insets.put("top", Math.round(topPx / density));
            insets.put("bottom", Math.round(bottomPx / density));
            insets.put("left", Math.round(leftPx / density));
            insets.put("right", Math.round(rightPx / density));

            Log.d(TAG, "getSafeAreaInsets: topPx=" + topPx + " bottomPx=" + bottomPx
                    + " density=" + density
                    + " -> top=" + insets.get("top") + "dp"
                    + " bottom=" + insets.get("bottom") + "dp"
                    + " left=" + insets.get("left") + "dp"
                    + " right=" + insets.get("right") + "dp");
        } else {
            insets.put("top", 0);
            insets.put("bottom", 0);
            insets.put("left", 0);
            insets.put("right", 0);
            Log.w(TAG, "getSafeAreaInsets: windowInsets is null");
        }

        return insets;
    }

    /**
     * Reapply the current style and colors after showing bars.
     * This ensures colors are preserved when hiding and then showing.
     */
    private void reapplyCurrentStyle(Activity activity) {
        Log.d(TAG, "reapplyCurrentStyle: style=" + currentStyle + ", colorHex=" + currentColorHex);
        Window window = activity.getWindow();

        // Reapply icon appearance
        if ("LIGHT".equalsIgnoreCase(currentStyle)) {
            setLightStatusBarIcons(window, true);
        } else if ("DARK".equalsIgnoreCase(currentStyle)) {
            setLightStatusBarIcons(window, false);
        } else if ("CUSTOM".equalsIgnoreCase(currentStyle)) {
            int parsed = parseColorOrDefault(currentColorHex, Color.BLACK);
            boolean isLight = isEffectiveLightColor(parsed);
            setLightStatusBarIcons(window, isLight);
        } else {
            // Default: Auto-detect based on system theme
            boolean isSystemDarkMode = isSystemInDarkMode(activity);
            setLightStatusBarIcons(window, !isSystemDarkMode);
        }

        // Reapply colors
        applyStatusBarBackground(activity, currentStatusBarColor);
        applyNavigationBarBackground(activity, currentNavBarColor);
    }

    private void setLightStatusBarIcons(Window window, boolean light) {
        Log.d(TAG, "setLightStatusBarIcons: light=" + light + ", API=" + Build.VERSION.SDK_INT);
        View decorView = window.getDecorView();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // API 30+ - Use WindowInsetsController
            WindowInsetsController controller = window.getInsetsController();
            if (controller == null) {
                Log.w(TAG, "setLightStatusBarIcons: WindowInsetsController is null");
                return;
            }
            int mask = WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS
                    | WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS;
            controller.setSystemBarsAppearance(light ? mask : 0, mask);
            Log.d(TAG, "setLightStatusBarIcons: applied using WindowInsetsController (API 30+)");
        } else {
            int flags = decorView.getSystemUiVisibility();
            if (light) {
                // Light background -> dark icons
                flags |= View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
                flags |= View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR;
                Log.d(TAG, "setLightStatusBarIcons: set light icons (dark text) (API 29)");
            } else {
                // Dark background -> light icons
                flags &= ~View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
                flags &= ~View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR;
                Log.d(TAG, "setLightStatusBarIcons: set dark icons (light text) (API 29)");
            }
            decorView.setSystemUiVisibility(flags);
        }
    }

    private void applyStatusBarBackground(Activity activity, @ColorInt int color) {
        Log.d(TAG, "applyStatusBarBackground: color=#" + Integer.toHexString(color) + ", API=" + Build.VERSION.SDK_INT);
        // Use overlay views for all API levels for consistent behavior
        ensureStatusBarOverlay(activity, color);
    }

    private void applyNavigationBarBackground(Activity activity, @ColorInt int color) {
        Log.d(TAG, "applyNavigationBarBackground: color=#" + Integer.toHexString(color) + ", API="
                + Build.VERSION.SDK_INT);
        // Use overlay views for all API levels for consistent behavior
        ensureNavBarOverlay(activity, color);
    }

    private void ensureStatusBarOverlay(Activity activity, @ColorInt int color) {
        Log.d(TAG, "ensureStatusBarOverlay: color=#" + Integer.toHexString(color));
        ViewGroup decorView = (ViewGroup) activity.getWindow().getDecorView();
        View existing = decorView.findViewWithTag(STATUS_BAR_OVERLAY_TAG);

        // Get current status bar height synchronously for immediate sizing
        int initialHeight = 0;
        WindowInsetsCompat rootInsets = ViewCompat.getRootWindowInsets(decorView);
        if (rootInsets != null) {
            initialHeight = rootInsets.getInsets(WindowInsetsCompat.Type.statusBars()).top;
        }

        if (existing == null) {
            Log.d(TAG, "ensureStatusBarOverlay: creating new overlay, initialHeight=" + initialHeight);
            View overlay = new View(activity);
            overlay.setTag(STATUS_BAR_OVERLAY_TAG);
            overlay.setBackgroundColor(color);

            FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    initialHeight);
            overlay.setLayoutParams(lp);

            decorView.addView(overlay);
            // Sizing updates are handled by the unified listener in ensureEdgeToEdgeConfigured
            decorView.requestApplyInsets();
        } else {
            Log.d(TAG, "ensureStatusBarOverlay: updating existing overlay");
            existing.setBackgroundColor(color);
            // Update height if it was 0 (listener hadn't fired yet)
            ViewGroup.LayoutParams params = existing.getLayoutParams();
            if (params.height == 0 && initialHeight > 0) {
                params.height = initialHeight;
                existing.setLayoutParams(params);
                Log.d(TAG, "ensureStatusBarOverlay: fixed height to " + initialHeight);
            }
        }
    }

    private void removeStatusBarOverlayIfPresent(Activity activity) {
        Log.d(TAG, "removeStatusBarOverlayIfPresent");
        ViewGroup decorView = (ViewGroup) activity.getWindow().getDecorView();
        View existing = decorView.findViewWithTag(STATUS_BAR_OVERLAY_TAG);
        if (existing != null) {
            Log.d(TAG, "removeStatusBarOverlayIfPresent: removing overlay");
            decorView.removeView(existing);
        }
    }

    private void ensureNavBarOverlay(Activity activity, @ColorInt int color) {
        Log.d(TAG, "ensureNavBarOverlay: color=#" + Integer.toHexString(color));
        ViewGroup decorView = (ViewGroup) activity.getWindow().getDecorView();
        View existing = decorView.findViewWithTag(NAV_BAR_OVERLAY_TAG);

        // Get current nav bar height synchronously for immediate sizing
        int initialHeight = 0;
        WindowInsetsCompat rootInsets = ViewCompat.getRootWindowInsets(decorView);
        if (rootInsets != null) {
            initialHeight = rootInsets.getInsets(WindowInsetsCompat.Type.navigationBars()).bottom;
        }

        if (existing == null) {
            Log.d(TAG, "ensureNavBarOverlay: creating new overlay, initialHeight=" + initialHeight);
            View overlay = new View(activity);
            overlay.setTag(NAV_BAR_OVERLAY_TAG);
            overlay.setBackgroundColor(color);

            FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    initialHeight);
            lp.gravity = Gravity.BOTTOM;
            overlay.setLayoutParams(lp);

            decorView.addView(overlay);
            // Sizing updates are handled by the unified listener in ensureEdgeToEdgeConfigured
            decorView.requestApplyInsets();
        } else {
            Log.d(TAG, "ensureNavBarOverlay: updating existing overlay");
            existing.setBackgroundColor(color);
            // Update height if it was 0 (listener hadn't fired yet)
            ViewGroup.LayoutParams params = existing.getLayoutParams();
            if (params.height == 0 && initialHeight > 0) {
                params.height = initialHeight;
                existing.setLayoutParams(params);
                Log.d(TAG, "ensureNavBarOverlay: fixed height to " + initialHeight);
            }
        }
    }

    private void removeNavBarOverlayIfPresent(Activity activity) {
        Log.d(TAG, "removeNavBarOverlayIfPresent");
        ViewGroup decorView = (ViewGroup) activity.getWindow().getDecorView();
        View existing = decorView.findViewWithTag(NAV_BAR_OVERLAY_TAG);
        if (existing != null) {
            Log.d(TAG, "removeNavBarOverlayIfPresent: removing overlay");
            decorView.removeView(existing);
        }
    }

    private void applyWindowBackground(Activity activity, @ColorInt int color) {
        Log.d(TAG, "applyWindowBackground: color=#" + Integer.toHexString(color));
        View decorView = activity.getWindow().getDecorView();
        decorView.setBackgroundColor(color);
    }

    /**
     * Makes the status bar and navigation bar backgrounds transparent.
     * This allows content to show through when the bars are hidden.
     */
    private void makeStatusBarBackgroundTransparent(Activity activity) {
        Log.d(TAG, "makeStatusBarBackgroundTransparent: API=" + Build.VERSION.SDK_INT);
        ViewGroup decorView = (ViewGroup) activity.getWindow().getDecorView();

        View statusBarOverlay = decorView.findViewWithTag(STATUS_BAR_OVERLAY_TAG);
        if (statusBarOverlay != null) {
            statusBarOverlay.setBackgroundColor(Color.TRANSPARENT);
            Log.d(TAG, "makeStatusBarBackgroundTransparent: status bar overlay made transparent");
        }

        View navBarOverlay = decorView.findViewWithTag(NAV_BAR_OVERLAY_TAG);
        if (navBarOverlay != null) {
            navBarOverlay.setBackgroundColor(Color.TRANSPARENT);
            Log.d(TAG, "makeStatusBarBackgroundTransparent: navigation bar overlay made transparent");
        }
    }

    /**
     * Restores the status bar and navigation bar backgrounds to their stored
     * colors.
     * Called when showing the bars after they were hidden.
     */
    private void restoreStatusBarBackground(Activity activity) {
        Log.d(TAG, "restoreStatusBarBackground: API=" + Build.VERSION.SDK_INT
                + ", currentStatusBarColor=#" + Integer.toHexString(currentStatusBarColor)
                + ", currentNavBarColor=#" + Integer.toHexString(currentNavBarColor));

        // Restore all backgrounds to their stored colors
        applyStatusBarBackground(activity, currentStatusBarColor);
        applyNavigationBarBackground(activity, currentNavBarColor);

        Log.d(TAG, "restoreStatusBarBackground: backgrounds restored");
    }

    @ColorInt
    private int parseColorOrDefault(@Nullable String color, @ColorInt int def) {
        if (color == null) {
            Log.d(TAG, "parseColorOrDefault: color is null, using default");
            return def;
        }
        try {
            int parsed = parseHexColor(color);
            Log.d(TAG, "parseColorOrDefault: parsed color=" + color + " -> #" + Integer.toHexString(parsed));
            return parsed;
        } catch (IllegalArgumentException ex) {
            Log.w(TAG, "parseColorOrDefault: invalid color=" + color + ", using default");
            return def;
        }
    }

    /**
     * Parse hex color string similar to iOS implementation.
     * Handles both 6-digit (#RRGGBB) and 8-digit (#RRGGBBAA) formats.
     *
     * @param hex The hex color string (with or without # prefix)
     * @return The parsed color as an integer
     * @throws IllegalArgumentException if the color format is invalid
     */
    private int parseHexColor(String hex) throws IllegalArgumentException {
        String hexSanitized = hex.trim().replaceFirst("^#", "");

        if (hexSanitized.length() != 6 && hexSanitized.length() != 8) {
            throw new IllegalArgumentException("Invalid hex color length: " + hexSanitized.length());
        }

        try {
            long rgb = Long.parseLong(hexSanitized, 16);

            if (hexSanitized.length() == 6) {
                // 6-digit format: #RRGGBB (opaque)
                return (int) (0xFF000000L | rgb);
            } else {
                // 8-digit format: #RRGGBBAA
                int r = (int) ((rgb & 0xFF000000L) >> 24);
                int g = (int) ((rgb & 0x00FF0000L) >> 16);
                int b = (int) ((rgb & 0x0000FF00L) >> 8);
                int a = (int) (rgb & 0x000000FFL);

                return Color.argb(a, r, g, b);
            }
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid hex color format: " + hex, e);
        }
    }

    /**
     * Calculate effective brightness considering alpha channel.
     * For transparent colors, we assume they will be blended over a white
     * background.
     *
     * @param color The color to analyze
     * @return true if the effective color appears light, false if dark
     */
    private boolean isEffectiveLightColor(@ColorInt int color) {
        int alpha = Color.alpha(color);

        if (alpha == 255) {
            // Fully opaque - use standard luminance calculation
            return ColorUtils.calculateLuminance(color) > 0.5;
        }

        // For transparent colors, calculate effective color when blended over white
        // background
        float alphaRatio = alpha / 255.0f;
        int r = Color.red(color);
        int g = Color.green(color);
        int b = Color.blue(color);

        // Blend with white background (255, 255, 255)
        int effectiveR = (int) (r * alphaRatio + 255 * (1 - alphaRatio));
        int effectiveG = (int) (g * alphaRatio + 255 * (1 - alphaRatio));
        int effectiveB = (int) (b * alphaRatio + 255 * (1 - alphaRatio));

        int effectiveColor = Color.rgb(effectiveR, effectiveG, effectiveB);
        return ColorUtils.calculateLuminance(effectiveColor) > 0.5;
    }

    /**
     * Apply default status bar style based on system theme.
     * Automatically detects if the device is in light or dark mode and applies the
     * appropriate style.
     *
     * @param activity The activity to apply the style to
     */
    public void applyDefaultStyle(Activity activity) {
        boolean isDarkMode = isSystemInDarkMode(activity);
        String style = isDarkMode ? "DARK" : "LIGHT";
        Log.d(TAG, "applyDefaultStyle: detected system theme=" + (isDarkMode ? "dark" : "light") + ", applying style="
                + style);
        setStyle(activity, style, null);
    }

    /**
     * Check if the system is currently in dark mode.
     *
     * @param activity The activity to check the configuration from
     * @return true if system is in dark mode, false otherwise
     */
    private boolean isSystemInDarkMode(Activity activity) {
        int nightModeFlags = activity.getResources().getConfiguration().uiMode
                & android.content.res.Configuration.UI_MODE_NIGHT_MASK;
        boolean isDarkMode = nightModeFlags == android.content.res.Configuration.UI_MODE_NIGHT_YES;
        Log.d(TAG, "isSystemInDarkMode: " + isDarkMode);
        return isDarkMode;
    }
}
