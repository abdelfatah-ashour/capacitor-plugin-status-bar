package com.cap.plugins.statusbar;

import android.app.Activity;
import android.graphics.Color;
import android.os.Build;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.view.WindowManager;
import android.widget.FrameLayout;

import androidx.annotation.ColorInt;
import androidx.annotation.Nullable;
import androidx.core.graphics.ColorUtils;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;

import com.getcapacitor.Plugin;

public class CapacitorStatusBar extends Plugin {
    private static final String TAG = "CapacitorStatusBar";
    private static final String STATUS_BAR_OVERLAY_TAG = "capacitor_status_bar_overlay";
    private static final String NAV_BAR_OVERLAY_TAG = "capacitor_navigation_bar_overlay";

    private String currentStyle = "LIGHT";
    private String currentColorHex = null;
    private int currentStatusBarColor = Color.BLACK;
    private int currentNavBarColor = Color.BLACK;
    private boolean statusBarHidden = false;
    private boolean navBarHidden = false;

    private boolean isAndroid15OrAbove() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM;
    }

    private boolean supportsInsetsController() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.R;
    }

    private boolean supportsContrastControl() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q;
    }

    private void applyLegacyContrastPolicy(Window window) {
        if (!isAndroid15OrAbove() && supportsContrastControl()) {
            window.setStatusBarContrastEnforced(false);
            window.setNavigationBarContrastEnforced(false);
        }
    }

    /**
     * Configure layout behavior for the current API level. Called once from
     * CapacitorStatusBarPlugin.load().
     */
    public void ensureEdgeToEdgeConfigured(Activity activity, @Nullable View webView) {
        Window window = activity.getWindow();
        View decorView = window.getDecorView();
        configureWebview(window, decorView);
    }

    private void configureWebview(Window window, View decorView) {
        if (isAndroid15OrAbove()) {

            // Android 15+: edge-to-edge. Overlay views paint the bar regions;
            // JS gets real insets.
            WindowCompat.setDecorFitsSystemWindows(window, false);

            window.setStatusBarColor(Color.TRANSPARENT);
            window.setNavigationBarColor(Color.TRANSPARENT);
            window.setStatusBarContrastEnforced(false);
            window.setNavigationBarContrastEnforced(false);

            ViewCompat.setOnApplyWindowInsetsListener(decorView, (v, insets) -> {
                int top = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top;
                int navBottom = insets.getInsets(WindowInsetsCompat.Type.navigationBars()).bottom;
                boolean imeVisible = insets.isVisible(WindowInsetsCompat.Type.ime());

                View statusOverlay = ((ViewGroup) v).findViewWithTag(STATUS_BAR_OVERLAY_TAG);
                if (statusOverlay != null) {
                    ViewGroup.LayoutParams params = statusOverlay.getLayoutParams();
                    if (params.height != top) {
                        params.height = top;
                        statusOverlay.setLayoutParams(params);
                    }
                }

                int overlayBottom = imeVisible ? 0 : navBottom;
                View navOverlay = ((ViewGroup) v).findViewWithTag(NAV_BAR_OVERLAY_TAG);
                if (navOverlay != null) {
                    ViewGroup.LayoutParams params = navOverlay.getLayoutParams();
                    if (params.height != overlayBottom) {
                        params.height = overlayBottom;
                        navOverlay.setLayoutParams(params);
                    }
                }

                return insets;
            });

            decorView.requestApplyInsets();
            Log.d(TAG, "ensureEdgeToEdgeConfigured: edge-to-edge (API " + Build.VERSION.SDK_INT + ")");
        } else {
            // API < 35: keep WebView inside the safe area. System draws bars.
            WindowCompat.setDecorFitsSystemWindows(window, true);

            // Ensure the system draws bar backgrounds with the colors we set
            // (not a translucent scrim).
            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS
                    | WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION);
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);

            applyLegacyContrastPolicy(window);
            Log.d(TAG, "ensureEdgeToEdgeConfigured: fitted layout (API " + Build.VERSION.SDK_INT + ")");
        }
    }

    private void configureAndroid14OrBelow(Window window) {
        // API < 35: keep WebView inside the safe area. System draws bars.
        WindowCompat.setDecorFitsSystemWindows(window, true);

        // Ensure the system draws bar backgrounds with the colors we set
        // (not a translucent scrim).
        window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS
                | WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION);
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);

        applyLegacyContrastPolicy(window);
        Log.d(TAG, "ensureEdgeToEdgeConfigured: fitted layout (API " + Build.VERSION.SDK_INT + ")");
    }

    public void setOverlaysWebView(Activity activity, boolean overlay) {
        Log.d(TAG, "setOverlaysWebView: no-op on Android, overlay=" + overlay);
    }

    public void showStatusBar(Activity activity, boolean animated) {
        Log.d(TAG, "showStatusBar: animated=" + animated + ", API=" + Build.VERSION.SDK_INT);
        Window window = activity.getWindow();
        View decorView = window.getDecorView();
        statusBarHidden = false;
        navBarHidden = false;

        if (!isAndroid15OrAbove()) {
            WindowCompat.setDecorFitsSystemWindows(window, true);
        }

        // Clear fullscreen flag so bars can appear again.
        window.clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);

        WindowInsetsControllerCompat controller = WindowCompat.getInsetsController(window, decorView);
        controller.show(WindowInsetsCompat.Type.statusBars());
        controller.show(WindowInsetsCompat.Type.navigationBars());

        // Restore overlays so colors reappear on both legacy and edge-to-edge.
        ViewGroup dv = (ViewGroup) decorView;
        View statusOverlay = dv.findViewWithTag(STATUS_BAR_OVERLAY_TAG);
        if (statusOverlay != null) {
            statusOverlay.setVisibility(View.VISIBLE);
        }
        View navOverlay = dv.findViewWithTag(NAV_BAR_OVERLAY_TAG);
        if (navOverlay != null) {
            navOverlay.setVisibility(View.VISIBLE);
        }

        reapplyCurrentStyle(activity);
    }

    public void hideStatusBar(Activity activity) {
        Log.d(TAG, "hideStatusBar: API=" + Build.VERSION.SDK_INT);
        Window window = activity.getWindow();
        View decorView = window.getDecorView();
        statusBarHidden = true;
        navBarHidden = true;

        // Edge-to-edge so the WebView extends into the freed bar regions.
        WindowCompat.setDecorFitsSystemWindows(window, false);

        // Clear flags that can block the bars from hiding.
        window.clearFlags(WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN);
        window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS
                | WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION);

        ViewGroup dv = (ViewGroup) decorView;
        View statusOverlay = dv.findViewWithTag(STATUS_BAR_OVERLAY_TAG);
        if (statusOverlay != null) {
            statusOverlay.setVisibility(View.GONE);
        }
        View navOverlay = dv.findViewWithTag(NAV_BAR_OVERLAY_TAG);
        if (navOverlay != null) {
            navOverlay.setVisibility(View.GONE);
        }

        hideSystemBarsNow(window, decorView);

        window.setStatusBarColor(Color.TRANSPARENT);
        window.setNavigationBarColor(Color.TRANSPARENT);

        // Re-apply on next frame to beat any post-layout reapplication from
        // the framework or other plugins that may re-show bars.
        decorView.post(() -> hideSystemBarsNow(window, decorView));
    }

    private void hideSystemBarsNow(Window window, View decorView) {
        WindowInsetsControllerCompat controller = WindowCompat.getInsetsController(window, decorView);
        controller.setSystemBarsBehavior(
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
        controller.hide(WindowInsetsCompat.Type.statusBars());
        controller.hide(WindowInsetsCompat.Type.navigationBars());

        // Belt-and-suspenders: on API 30–34, set FLAG_FULLSCREEN as a fallback
        // in case the controller call is ignored by theme/manifest state.
        if (!isAndroid15OrAbove()) {
            window.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        }

        // Legacy path (API < 30): also toggle system UI visibility flags.
        if (!supportsInsetsController()) {
            decorView.setSystemUiVisibility(
                    decorView.getSystemUiVisibility()
                            | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                            | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);
        }
    }

    public void setStyle(Activity activity, String style, @Nullable String colorHex) {
        Log.d(TAG, "setStyle: style=" + style + ", colorHex=" + colorHex);
        Window window = activity.getWindow();

        if (!isAndroid15OrAbove()) {
            statusBarHidden = false;
            navBarHidden = false;
            ensureLegacyOpaqueSystemBars(window);
        }

        currentStyle = style;
        currentColorHex = colorHex;

        boolean lightBackground;
        int statusBarColor;
        int navBarColor;

        if ("CUSTOM".equalsIgnoreCase(style) && colorHex != null) {
            int color = parseColorOrDefault(colorHex, Color.BLACK);
            lightBackground = isEffectiveLightColor(color);
            statusBarColor = color;
            navBarColor = color;
        } else if ("LIGHT".equalsIgnoreCase(style)) {
            lightBackground = true;
            statusBarColor = Color.WHITE;
            navBarColor = Color.WHITE;
        } else if ("DARK".equalsIgnoreCase(style)) {
            lightBackground = false;
            statusBarColor = Color.BLACK;
            navBarColor = Color.BLACK;
        } else {
            boolean isSystemDarkMode = isSystemInDarkMode(activity);
            lightBackground = !isSystemDarkMode;
            statusBarColor = isSystemDarkMode ? Color.BLACK : Color.WHITE;
            navBarColor = statusBarColor;
        }

        currentStatusBarColor = statusBarColor;
        currentNavBarColor = navBarColor;

        if (!isAndroid15OrAbove()) {
            // Clear any translucent scrim the framework/theme may have applied,
            // and ensure the system draws bar backgrounds with our colors.
            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS
                    | WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION);
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        }

        applyLegacyContrastPolicy(window);
        setLightStatusBarIcons(window, lightBackground);
        applyStatusBarBackground(activity, statusBarColor);
        applyNavigationBarBackground(activity, navBarColor);

        applyLegacyContrastPolicy(window);

        if (!isAndroid15OrAbove()) {
            // Re-apply on the next frame to win against same-tick framework/plugin updates.
            window.getDecorView().post(() -> {
                window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS
                        | WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION);
                window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
                applyLegacyContrastPolicy(window);
                window.setStatusBarColor(statusBarColor);
                window.setNavigationBarColor(navBarColor);
                setLightStatusBarIcons(window, lightBackground);
            });
        }
    }

    public java.util.Map<String, Integer> getSafeAreaInsets(Activity activity) {
        java.util.Map<String, Integer> insets = new java.util.HashMap<>();
        float density = activity.getResources().getDisplayMetrics().density;
        View decorView = activity.getWindow().getDecorView();
        WindowInsetsCompat windowInsets = ViewCompat.getRootWindowInsets(decorView);

        if (windowInsets == null) {
            insets.put("top", 0);
            insets.put("bottom", 0);
            insets.put("left", 0);
            insets.put("right", 0);
            Log.w(TAG, "getSafeAreaInsets: windowInsets is null");
            return insets;
        }

        androidx.core.graphics.Insets statusBars = windowInsets
                .getInsetsIgnoringVisibility(WindowInsetsCompat.Type.statusBars());
        androidx.core.graphics.Insets navBars = windowInsets
                .getInsetsIgnoringVisibility(WindowInsetsCompat.Type.navigationBars());
        androidx.core.graphics.Insets displayCutout = windowInsets.getInsets(WindowInsetsCompat.Type.displayCutout());

        int topPx;
        int bottomPx;
        int leftPx;
        int rightPx;

        if (isAndroid15OrAbove()) {
            topPx = Math.max(statusBars.top, displayCutout.top);
            bottomPx = Math.max(navBars.bottom, displayCutout.bottom);
            leftPx = Math.max(navBars.left, displayCutout.left);
            rightPx = Math.max(navBars.right, displayCutout.right);
        } else {
            topPx = statusBarHidden ? statusBars.top : 0;
            bottomPx = navBarHidden ? navBars.bottom : 0;
            leftPx = 0;
            rightPx = 0;
        }

        insets.put("top", Math.round(topPx / density));
        insets.put("bottom", Math.round(bottomPx / density));
        insets.put("left", Math.round(leftPx / density));
        insets.put("right", Math.round(rightPx / density));

        Log.d(TAG, "getSafeAreaInsets: top=" + insets.get("top") + "px"
                + " bottom=" + insets.get("bottom") + "px"
                + " left=" + insets.get("left") + "px"
                + " right=" + insets.get("right") + "px"
                + " (statusHidden=" + statusBarHidden + ", navHidden=" + navBarHidden + ")");

        return insets;
    }

    public void showNavigationBar(Activity activity, boolean animated) {
        Log.d(TAG, "showNavigationBar: animated=" + animated + ", API=" + Build.VERSION.SDK_INT);
        Window window = activity.getWindow();
        View decorView = window.getDecorView();
        navBarHidden = false;

        if (!isAndroid15OrAbove() && !statusBarHidden) {
            WindowCompat.setDecorFitsSystemWindows(window, true);
        }

        if (supportsInsetsController()) {
            WindowInsetsController controller = window.getInsetsController();
            if (controller != null) {
                controller.show(WindowInsets.Type.navigationBars());
                controller.setSystemBarsBehavior(
                        WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
            }
        } else {
            int flags = decorView.getSystemUiVisibility();
            flags &= ~View.SYSTEM_UI_FLAG_HIDE_NAVIGATION;
            flags &= ~View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;
            flags &= ~View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION;
            decorView.setSystemUiVisibility(flags);
        }

        ViewGroup dv = (ViewGroup) decorView;
        View navOverlay = dv.findViewWithTag(NAV_BAR_OVERLAY_TAG);
        if (navOverlay != null) {
            navOverlay.setVisibility(View.VISIBLE);
        }

        applyNavigationBarBackground(activity, currentNavBarColor);
    }

    public void hideNavigationBar(Activity activity, String animation) {
        Log.d(TAG, "hideNavigationBar: animation=" + animation + ", API=" + Build.VERSION.SDK_INT);
        Window window = activity.getWindow();
        View decorView = window.getDecorView();
        navBarHidden = true;

        if (!isAndroid15OrAbove()) {
            WindowCompat.setDecorFitsSystemWindows(window, false);
        }

        if (supportsInsetsController()) {
            WindowInsetsController controller = window.getInsetsController();
            if (controller != null) {
                controller.hide(WindowInsets.Type.navigationBars());
                controller.setSystemBarsBehavior(
                        WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
            }
        } else {
            decorView.setSystemUiVisibility(
                    decorView.getSystemUiVisibility()
                            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
        }

        ViewGroup dv = (ViewGroup) decorView;
        View navOverlay = dv.findViewWithTag(NAV_BAR_OVERLAY_TAG);
        if (navOverlay != null) {
            navOverlay.setVisibility(View.GONE);
        }

        if (!isAndroid15OrAbove()) {
            window.setNavigationBarColor(Color.TRANSPARENT);
        }
    }

    private void reapplyCurrentStyle(Activity activity) {
        Window window = activity.getWindow();

        if (!isAndroid15OrAbove() && !statusBarHidden && !navBarHidden) {
            ensureLegacyOpaqueSystemBars(window);
        }

        boolean light;
        if ("LIGHT".equalsIgnoreCase(currentStyle)) {
            light = true;
        } else if ("DARK".equalsIgnoreCase(currentStyle)) {
            light = false;
        } else if ("CUSTOM".equalsIgnoreCase(currentStyle)) {
            light = isEffectiveLightColor(parseColorOrDefault(currentColorHex, Color.BLACK));
        } else {
            light = !isSystemInDarkMode(activity);
        }

        applyLegacyContrastPolicy(window);
        setLightStatusBarIcons(window, light);
        applyStatusBarBackground(activity, currentStatusBarColor);
        applyNavigationBarBackground(activity, currentNavBarColor);

        applyLegacyContrastPolicy(window);
    }

    private void ensureLegacyOpaqueSystemBars(Window window) {
        WindowCompat.setDecorFitsSystemWindows(window, true);

        // Ensure bar backgrounds are drawn opaquely on API < 35.
        window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS
                | WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION);
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);

        if (supportsInsetsController()) {
            WindowInsetsController controller = window.getInsetsController();
            if (controller != null) {
                controller.setSystemBarsBehavior(
                        WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
                controller.show(WindowInsets.Type.systemBars());
            }
        }

        if (!supportsInsetsController()) {
            View decorView = window.getDecorView();
            int flags = decorView.getSystemUiVisibility();
            flags &= ~View.SYSTEM_UI_FLAG_FULLSCREEN;
            flags &= ~View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN;
            flags &= ~View.SYSTEM_UI_FLAG_HIDE_NAVIGATION;
            flags &= ~View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION;
            flags &= ~View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;
            decorView.setSystemUiVisibility(flags);
        }

        applyLegacyContrastPolicy(window);

        window.getDecorView().requestApplyInsets();
    }

    private void setLightStatusBarIcons(Window window, boolean light) {
        View decorView = window.getDecorView();

        if (supportsInsetsController()) {
            WindowInsetsController controller = window.getInsetsController();
            if (controller == null) {
                return;
            }
            int mask = WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS
                    | WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS;
            controller.setSystemBarsAppearance(light ? mask : 0, mask);
        } else {
            int flags = decorView.getSystemUiVisibility();
            if (light) {
                flags |= View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
                flags |= View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR;
            } else {
                flags &= ~View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
                flags &= ~View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR;
            }
            decorView.setSystemUiVisibility(flags);
        }
    }

    private void applyStatusBarBackground(Activity activity, @ColorInt int color) {
        Window window = activity.getWindow();
        window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        if (supportsContrastControl()) {
            window.setStatusBarContrastEnforced(false);
        }
        // Paint status bar transparent; our overlay view paints the color so the
        // framework's contrast scrim cannot affect it.
        window.setStatusBarColor(Color.TRANSPARENT);
        ensureStatusBarOverlay(activity, color);
    }

    private void applyNavigationBarBackground(Activity activity, @ColorInt int color) {
        Window window = activity.getWindow();
        window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION);
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        if (supportsContrastControl()) {
            window.setNavigationBarContrastEnforced(false);
        }
        window.setNavigationBarColor(Color.TRANSPARENT);
        ensureNavBarOverlay(activity, color);
    }

    private void ensureStatusBarOverlay(Activity activity, @ColorInt int color) {
        ViewGroup decorView = (ViewGroup) activity.getWindow().getDecorView();
        View existing = decorView.findViewWithTag(STATUS_BAR_OVERLAY_TAG);

        int initialHeight = 0;
        WindowInsetsCompat rootInsets = ViewCompat.getRootWindowInsets(decorView);
        if (rootInsets != null) {
            initialHeight = rootInsets.getInsets(WindowInsetsCompat.Type.statusBars()).top;
        }

        if (existing == null) {
            View overlay = new View(activity);
            overlay.setTag(STATUS_BAR_OVERLAY_TAG);
            overlay.setBackgroundColor(color);

            FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    initialHeight);
            overlay.setLayoutParams(lp);

            decorView.addView(overlay);
            decorView.requestApplyInsets();
        } else {
            existing.setBackgroundColor(color);
            ViewGroup.LayoutParams params = existing.getLayoutParams();
            if (params.height == 0 && initialHeight > 0) {
                params.height = initialHeight;
                existing.setLayoutParams(params);
            }
        }
    }

    private void ensureNavBarOverlay(Activity activity, @ColorInt int color) {
        ViewGroup decorView = (ViewGroup) activity.getWindow().getDecorView();
        View existing = decorView.findViewWithTag(NAV_BAR_OVERLAY_TAG);

        int initialHeight = 0;
        WindowInsetsCompat rootInsets = ViewCompat.getRootWindowInsets(decorView);
        if (rootInsets != null) {
            initialHeight = rootInsets.getInsets(WindowInsetsCompat.Type.navigationBars()).bottom;
        }

        if (existing == null) {
            View overlay = new View(activity);
            overlay.setTag(NAV_BAR_OVERLAY_TAG);
            overlay.setBackgroundColor(color);

            FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    initialHeight);
            lp.gravity = Gravity.BOTTOM;
            overlay.setLayoutParams(lp);

            decorView.addView(overlay);
            decorView.requestApplyInsets();
        } else {
            existing.setBackgroundColor(color);
            ViewGroup.LayoutParams params = existing.getLayoutParams();
            if (params.height == 0 && initialHeight > 0) {
                params.height = initialHeight;
                existing.setLayoutParams(params);
            }
        }
    }

    @ColorInt
    private int parseColorOrDefault(@Nullable String color, @ColorInt int def) {
        if (color == null) {
            return def;
        }
        try {
            return parseHexColor(color);
        } catch (IllegalArgumentException ex) {
            Log.w(TAG, "parseColorOrDefault: invalid color=" + color + ", using default");
            return def;
        }
    }

    /**
     * Parse hex color string. Handles #RRGGBB and #RRGGBBAA formats.
     */
    private int parseHexColor(String hex) throws IllegalArgumentException {
        String hexSanitized = hex.trim().replaceFirst("^#", "");

        if (hexSanitized.length() != 6 && hexSanitized.length() != 8) {
            throw new IllegalArgumentException("Invalid hex color length: " + hexSanitized.length());
        }

        try {
            long rgb = Long.parseLong(hexSanitized, 16);

            if (hexSanitized.length() == 6) {
                return (int) (0xFF000000L | rgb);
            } else {
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

    private boolean isEffectiveLightColor(@ColorInt int color) {
        int alpha = Color.alpha(color);

        if (alpha == 255) {
            return ColorUtils.calculateLuminance(color) > 0.5;
        }

        float alphaRatio = alpha / 255.0f;
        int r = Color.red(color);
        int g = Color.green(color);
        int b = Color.blue(color);

        int effectiveR = (int) (r * alphaRatio + 255 * (1 - alphaRatio));
        int effectiveG = (int) (g * alphaRatio + 255 * (1 - alphaRatio));
        int effectiveB = (int) (b * alphaRatio + 255 * (1 - alphaRatio));

        return ColorUtils.calculateLuminance(Color.rgb(effectiveR, effectiveG, effectiveB)) > 0.5;
    }

    public void applyDefaultStyle(Activity activity) {
        boolean isDarkMode = isSystemInDarkMode(activity);
        setStyle(activity, isDarkMode ? "DARK" : "LIGHT", null);
    }

    private boolean isSystemInDarkMode(Activity activity) {
        int nightModeFlags = activity.getResources().getConfiguration().uiMode
                & android.content.res.Configuration.UI_MODE_NIGHT_MASK;
        return nightModeFlags == android.content.res.Configuration.UI_MODE_NIGHT_YES;
    }
}
