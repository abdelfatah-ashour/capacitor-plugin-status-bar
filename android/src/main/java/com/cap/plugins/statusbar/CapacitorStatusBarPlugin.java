package com.cap.plugins.statusbar;

import android.view.View;

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
        getActivity().runOnUiThread(() -> {
            View webView = getBridge().getWebView();
            implementation.ensureEdgeToEdgeConfigured(getActivity(), webView);
            implementation.applyDefaultStyle(getActivity());
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
            getActivity().runOnUiThread(() -> {
                implementation.showStatusBar(getActivity(), true);
                call.resolve();
            });
        } catch (Exception e) {
            call.reject(e.getMessage());
        }
    }

    @PluginMethod
    public void hide(PluginCall call) {
        try {
            getActivity().runOnUiThread(() -> {
                implementation.hideStatusBar(getActivity());
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
    public void showNavigationBar(PluginCall call) {
        try {
            getActivity().runOnUiThread(() -> {
                implementation.showNavigationBar(getActivity(), true);
                call.resolve();
            });
        } catch (Exception e) {
            call.reject(e.getMessage());
        }
    }

    @PluginMethod
    public void hideNavigationBar(PluginCall call) {
        try {
            getActivity().runOnUiThread(() -> {
                implementation.hideNavigationBar(getActivity(), "slide");
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
