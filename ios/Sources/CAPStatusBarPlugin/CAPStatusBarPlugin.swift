import Foundation
import Capacitor

/**
 * Please read the Capacitor iOS Plugin Development Guide
 * here: https://capacitorjs.com/docs/plugins/ios
 */
@objc(CAPStatusBarPlugin)
public class CAPStatusBarPlugin: CAPPlugin, CAPBridgedPlugin {
    public let identifier = "CAPStatusBarPlugin"
    public let jsName = "CAPStatusBar"
    public let pluginMethods: [CAPPluginMethod] = [
        CAPPluginMethod(name: "setStyle", returnType: CAPPluginReturnPromise),
        CAPPluginMethod(name: "show", returnType: CAPPluginReturnPromise),
        CAPPluginMethod(name: "hide", returnType: CAPPluginReturnPromise),
        CAPPluginMethod(name: "setOverlaysWebView", returnType: CAPPluginReturnPromise),
        CAPPluginMethod(name: "setBackground", returnType: CAPPluginReturnPromise),
        CAPPluginMethod(name: "setStatusBarColor", returnType: CAPPluginReturnPromise),
        CAPPluginMethod(name: "getSafeAreaInsets", returnType: CAPPluginReturnPromise)
    ]
    private let implementation = CAPStatusBar()

    override public func load() {
        super.load()
        // Apply default style based on system theme on plugin load
        implementation.applyDefaultStyle()
    }

    @objc func setStyle(_ call: CAPPluginCall) {
        guard let style = call.getString("style") else {
            call.reject("style is required")
            return
        }
        let color = call.getString("color")
        implementation.setStyle(style: style, colorHex: color)
        call.resolve()
    }

    @objc func show(_ call: CAPPluginCall) {
        let animated = call.getBool("animated") ?? true
        implementation.show(animated: animated)
        call.resolve()
    }

    @objc func hide(_ call: CAPPluginCall) {
        let animated = call.getBool("animated") ?? true
        implementation.hide(animated: animated)
        call.resolve()
    }

    @objc func setOverlaysWebView(_ call: CAPPluginCall) {
        let value = call.getBool("value") ?? true
        implementation.setOverlaysWebView(value: value)
        call.resolve()
    }

    @objc func setBackground(_ call: CAPPluginCall) {
        guard let color = call.getString("color") else {
            call.reject("color is required")
            return
        }
        implementation.setBackground(colorHex: color)
        call.resolve()
    }

    @objc func setStatusBarColor(_ call: CAPPluginCall) {
        guard let color = call.getString("color") else {
            call.reject("color is required")
            return
        }
        implementation.setStatusBarColor(colorHex: color)
        call.resolve()
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
