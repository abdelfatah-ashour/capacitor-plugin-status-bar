import Foundation
import Capacitor

/**
 * Please read the Capacitor iOS Plugin Development Guide
 * here: https://capacitorjs.com/docs/plugins/ios
 */
@objc(CapacitorStatusBarPlugin)
public class CapacitorStatusBarPlugin: CAPPlugin, CAPBridgedPlugin {
    public let identifier = "CapacitorStatusBarPlugin"
    public let jsName = "CapacitorStatusBar"
    public let pluginMethods: [CAPPluginMethod] = [
        CAPPluginMethod(name: "setStyle", returnType: CAPPluginReturnPromise),
        CAPPluginMethod(name: "show", returnType: CAPPluginReturnPromise),
        CAPPluginMethod(name: "hide", returnType: CAPPluginReturnPromise),
        CAPPluginMethod(name: "setOverlaysWebView", returnType: CAPPluginReturnPromise),
        CAPPluginMethod(name: "showNavigationBar", returnType: CAPPluginReturnPromise),
        CAPPluginMethod(name: "hideNavigationBar", returnType: CAPPluginReturnPromise),
        CAPPluginMethod(name: "getSafeAreaInsets", returnType: CAPPluginReturnPromise)
    ]
    private let implementation = CapacitorStatusBar()

    override public func load() {
        super.load()
        implementation.setWebView(bridge?.webView)
        // Apply default style based on system theme on plugin load
        implementation.applyDefaultStyle()
    }

    @objc func setStyle(_ call: CAPPluginCall) {
        guard let style = call.getString("style") else {
            call.reject("style is required")
            return
        }
        let color = call.getString("color")
        implementation.setStyle(style: style, colorHex: color) {
            call.resolve()
        }
    }

    @objc func show(_ call: CAPPluginCall) {
        implementation.show(animated: true) {
            call.resolve()
        }
    }

    @objc func hide(_ call: CAPPluginCall) {
        implementation.hide(animation: "slide") {
            call.resolve()
        }
    }

    @objc func setOverlaysWebView(_ call: CAPPluginCall) {
        guard let value = call.getBool("value") else {
            call.reject("value is required")
            return
        }
        implementation.setOverlaysWebView(value: value) {
            call.resolve()
        }
    }

    @objc func showNavigationBar(_ call: CAPPluginCall) {
        implementation.showNavigationBar(animated: true) {
            call.resolve()
        }
    }

    @objc func hideNavigationBar(_ call: CAPPluginCall) {
        implementation.hideNavigationBar(animation: "slide") {
            call.resolve()
        }
    }

    @objc func getSafeAreaInsets(_ call: CAPPluginCall) {
        implementation.getSafeAreaInsets { insets in
            call.resolve([
                "top": insets["top"] ?? 0,
                "bottom": insets["bottom"] ?? 0,
                "left": insets["left"] ?? 0,
                "right": insets["right"] ?? 0
            ])
        }
    }
}
