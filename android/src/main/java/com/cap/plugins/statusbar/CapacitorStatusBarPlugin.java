package com.cap.plugins.statusbar;

import android.os.Build;
import android.view.View;

import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import com.getcapacitor.annotation.CapacitorPlugin;

@CapacitorPlugin(name = "CapacitorStatusBar")
public class CapacitorStatusBarPlugin extends Plugin {
    private final CapacitorStatusBar implementation = new CapacitorStatusBar();

    @Override
    public void load() {
        super.load();
        // Apply default style based on system theme on plugin load
        getActivity().runOnUiThread(() -> {
            implementation.ensureEdgeToEdgeConfigured(getActivity());
            implementation.applyDefaultStyle(getActivity());

            // On Android 15+ (API 35+), edge-to-edge is enforced and the WebView content
            // extends under system bars. Apply padding so Ionic content stays in the safe area.
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) {
                View webView = getBridge().getWebView();
                ViewCompat.setOnApplyWindowInsetsListener(webView, (v, insets) -> {
                    Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                    v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
                    return insets;
                });
            }
        });
    }

    @PluginMethod
    public void setStyle(PluginCall call) {
        try {
            String style = call.getString("style");
            String color = call.getString("color");
            if (style == null) {
                call.reject("style is required");
                return;
            }
            getActivity().runOnUiThread(() -> {
                implementation.setStyle(getActivity(), style, color);
                call.resolve();
            });
        } catch (Exception e) {
            call.reject(e.getMessage());
        }
    }

    @PluginMethod
    public void show(PluginCall call) {
        try {
            boolean animated = Boolean.TRUE.equals(call.getBoolean("animated", true));
            getActivity().runOnUiThread(() -> {
                implementation.showStatusBar(getActivity(), animated);
                call.resolve();
            });
        } catch (Exception e) {
            call.reject(e.getMessage());
        }
    }

    @PluginMethod
    public void hide(PluginCall call) {
        try {
            String animation = call.getString("animation");
            if (animation == null) {
                call.reject("animation is required");
                return;
            }
            getActivity().runOnUiThread(() -> {
                implementation.hideStatusBar(getActivity(), animation);
                call.resolve();
            });
        } catch (Exception e) {
            call.reject(e.getMessage());
        }
    }

    @PluginMethod
    public void setOverlaysWebView(PluginCall call) {
        try {
            Boolean value = call.getBoolean("value");
            if (value == null) {
                call.reject("value is required");
                return;
            }
            getActivity().runOnUiThread(() -> {
                implementation.setOverlaysWebView(getActivity(), value);
                call.resolve();
            });
        } catch (Exception e) {
            call.reject(e.getMessage());
        }
    }

    @PluginMethod
    public void setBackground(PluginCall call) {
        try {
            String color = call.getString("color");
            if (color == null) {
                call.reject("color is required");
                return;
            }
            getActivity().runOnUiThread(() -> {
                implementation.setBackground(getActivity(), color);
                call.resolve();
            });
        } catch (Exception e) {
            call.reject(e.getMessage());
        }
    }

    @PluginMethod
    public void getSafeAreaInsets(PluginCall call) {
        try {
            getActivity().runOnUiThread(() -> {
                java.util.Map<String, Integer> insets = implementation.getSafeAreaInsets(getActivity());
                com.getcapacitor.JSObject result = new com.getcapacitor.JSObject();
                result.put("top", insets.get("top"));
                result.put("bottom", insets.get("bottom"));
                result.put("left", insets.get("left"));
                result.put("right", insets.get("right"));
                call.resolve(result);
            });
        } catch (Exception e) {
            call.reject(e.getMessage());
        }
    }
}
