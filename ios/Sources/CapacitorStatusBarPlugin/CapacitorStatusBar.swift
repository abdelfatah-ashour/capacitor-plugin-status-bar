import Foundation
import UIKit
import Capacitor

@objc public class CapacitorStatusBar: NSObject {
    // Tag to identify the status bar background view
    private static let statusBarViewTag = 38482458
    // Store the current background color to restore when showing
    private var currentBackgroundColor: UIColor?
    // Track whether overlays web view mode is active
    private var isOverlayMode = false
    // Keep a weak reference to the Capacitor WKWebView for layout updates
    private weak var webView: UIView?

    @objc public func setWebView(_ webView: UIView?) {
        self.webView = webView
    }

    @objc public func applyDefaultStyle() {
        DispatchQueue.main.async {
            let isDarkMode = self.isSystemInDarkMode()
            let style = isDarkMode ? "DARK" : "LIGHT"
            print("CapacitorStatusBar: Applying default style based on system theme - isDarkMode=\(isDarkMode), style=\(style)")
            self.setStyle(style: style, colorHex: nil)
        }
    }

    @objc public func setStyle(style: String, colorHex: String?, completion: (() -> Void)? = nil) {
        DispatchQueue.main.async {
            defer { completion?() }

            guard let windowScene = UIApplication.shared.connectedScenes.first as? UIWindowScene else { return }
            guard let window = windowScene.windows.first else { return }
            guard let statusBarManager = windowScene.statusBarManager else { return }
            guard let rootVC = window.rootViewController else { return }

            let upperStyle = style.uppercased()
            var backgroundColor: UIColor?
            var statusBarStyle: UIStatusBarStyle = .default

            // Determine the status bar style and background color
            if upperStyle == "LIGHT" {
                statusBarStyle = .darkContent
                backgroundColor = .white
            } else if upperStyle == "DARK" {
                statusBarStyle = .lightContent
                backgroundColor = .black
            } else if upperStyle == "CUSTOM" {
                if let colorHex = colorHex, let color = self.colorFromHex(colorHex) {
                    backgroundColor = color
                    let brightness = self.getColorBrightness(color)
                    statusBarStyle = brightness > 0.5 ? .darkContent : .lightContent
                } else {
                    statusBarStyle = .default
                    backgroundColor = nil
                }
            } else {
                statusBarStyle = .default
                backgroundColor = nil
            }

            // Set the status bar style via swizzled preferredStatusBarStyle
            CapacitorStatusBar.swizzleStatusBarStyleIfNeeded()
            CapacitorStatusBar.currentStatusBarStyle = statusBarStyle
            rootVC.setNeedsStatusBarAppearanceUpdate()

            // Store the background color for later restoration
            self.currentBackgroundColor = backgroundColor

            // Skip background color update when overlays web view is active
            if self.isOverlayMode {
                print("CapacitorStatusBar: setStyle - overlay mode active, skipping background color")
            } else {
                self.updateStatusBarBackgroundView(in: window,
                                                   height: statusBarManager.statusBarFrame.height,
                                                   color: backgroundColor)
            }

            print("CapacitorStatusBar: setStyle - style=\(upperStyle), backgroundColor=\(String(describing: backgroundColor)), statusBarStyle=\(statusBarStyle)")
        }
    }

    @objc public func show(animated: Bool, completion: (() -> Void)? = nil) {
        DispatchQueue.main.async {
            defer { completion?() }

            guard let windowScene = UIApplication.shared.connectedScenes.first as? UIWindowScene,
                  let window = windowScene.windows.first,
                  let rootVC = window.rootViewController else { return }

            if let statusBarManager = windowScene.statusBarManager {
                print("CapacitorStatusBar: show() - Current hidden state: \(statusBarManager.isStatusBarHidden)")
            }

            // Show status bar via swizzled prefersStatusBarHidden
            CapacitorStatusBar.swizzleStatusBarVisibilityIfNeeded()
            CapacitorStatusBar.currentStatusBarHidden = false
            rootVC.setNeedsStatusBarAppearanceUpdate()

            // Restore the background view color when showing (unless overlay mode is active)
            if !self.isOverlayMode {
                self.restoreStatusBarBackgroundColor()
            }

            self.updateWebViewLayout()
            DispatchQueue.main.asyncAfter(deadline: .now() + 0.1) {
                self.updateWebViewLayout()
            }
        }
    }

    @objc public func hide(animation: String, completion: (() -> Void)? = nil) {
        DispatchQueue.main.async {
            defer { completion?() }

            let animationType = animation.lowercased()

            if let windowScene = UIApplication.shared.connectedScenes.first as? UIWindowScene,
               let statusBarManager = windowScene.statusBarManager {
                print("CapacitorStatusBar: hide() - animation=\(animationType), Current hidden state: \(statusBarManager.isStatusBarHidden)")
            }

            if animationType == "fade" {
                // Fade mode: Make background transparent without hiding status bar
                print("CapacitorStatusBar: hide() - fade mode: making background transparent")
                self.makeStatusBarBackgroundTransparent()
            } else {
                // Slide mode (default): Hide the status bar completely
                if animationType != "slide" {
                    print("CapacitorStatusBar: hide() - unknown animation '\(animationType)', defaulting to slide")
                } else {
                    print("CapacitorStatusBar: hide() - slide mode: hiding status bar")
                }

                guard let windowScene = UIApplication.shared.connectedScenes.first as? UIWindowScene,
                      let window = windowScene.windows.first,
                      let rootVC = window.rootViewController else { return }

                CapacitorStatusBar.swizzleStatusBarVisibilityIfNeeded()
                CapacitorStatusBar.currentStatusBarHidden = true
                rootVC.setNeedsStatusBarAppearanceUpdate()

                self.makeStatusBarBackgroundTransparent()
            }

            self.updateWebViewLayout()
            DispatchQueue.main.asyncAfter(deadline: .now() + 0.1) {
                self.updateWebViewLayout()
            }
        }
    }

    // MARK: - Private Methods

    /// Updates or creates the status bar background view with the specified color.
    /// - Parameters:
    ///   - window: The window where the status bar view will be added
    ///   - height: The height of the status bar
    ///   - color: The background color (nil to remove the view)
    private func updateStatusBarBackgroundView(in window: UIWindow, height: CGFloat, color: UIColor?) {
        // Find existing status bar view
        let existingView = window.viewWithTag(CapacitorStatusBar.statusBarViewTag)

        if let color = color {
            // Create or update the status bar background view
            let statusBarView: UIView

            if let existing = existingView {
                statusBarView = existing
            } else {
                statusBarView = UIView(frame: CGRect(x: 0, y: 0, width: window.bounds.width, height: height))
                statusBarView.tag = CapacitorStatusBar.statusBarViewTag
                statusBarView.autoresizingMask = [.flexibleWidth]
                window.addSubview(statusBarView)
            }

            // Update the frame and color
            statusBarView.frame = CGRect(x: 0, y: 0, width: window.bounds.width, height: height)
            statusBarView.backgroundColor = color

            // Ensure the view is on top
            window.bringSubviewToFront(statusBarView)
        } else {
            // Remove the status bar view if color is nil
            existingView?.removeFromSuperview()
        }
    }

    @objc public func setOverlaysWebView(value: Bool, completion: (() -> Void)? = nil) {
        DispatchQueue.main.async {
            defer { completion?() }

            guard let windowScene = UIApplication.shared.connectedScenes.first as? UIWindowScene,
                  let window = windowScene.windows.first,
                  let statusBarManager = windowScene.statusBarManager else {
                print("CapacitorStatusBar: setOverlaysWebView - Unable to get window or status bar manager")
                return
            }

            self.isOverlayMode = value

            if value {
                let statusBarView = window.viewWithTag(CapacitorStatusBar.statusBarViewTag)
                statusBarView?.backgroundColor = .clear
                print("CapacitorStatusBar: setOverlaysWebView(true) - content extends behind status bar")
            } else {
                if let color = self.currentBackgroundColor {
                    self.updateStatusBarBackgroundView(in: window,
                                                       height: statusBarManager.statusBarFrame.height,
                                                       color: color)
                    print("CapacitorStatusBar: setOverlaysWebView(false) - restored background color")
                } else {
                    self.applyDefaultStyle()
                    print("CapacitorStatusBar: setOverlaysWebView(false) - applied default style from config")
                }
            }

            self.updateWebViewLayout()
            DispatchQueue.main.asyncAfter(deadline: .now() + 0.1) {
                self.updateWebViewLayout()
            }
        }
    }

    @objc public func getSafeAreaInsets(completion: @escaping ([String: CGFloat]) -> Void) {
        DispatchQueue.main.async {
            var insets: [String: CGFloat] = ["top": 0, "bottom": 0, "left": 0, "right": 0]

            if let windowScene = UIApplication.shared.connectedScenes.first as? UIWindowScene,
               let window = windowScene.windows.first(where: { $0.isKeyWindow }) ?? windowScene.windows.first {
                let safeAreaInsets = window.safeAreaInsets

                insets["top"] = self.sanitizedInsetValue(safeAreaInsets.top)
                let homeIndicatorHeight: CGFloat = safeAreaInsets.bottom > 0
                    ? min(13.0, safeAreaInsets.bottom)
                    : 0
                insets["bottom"] = self.sanitizedInsetValue(homeIndicatorHeight)
                insets["left"] = 0
                insets["right"] = 0

                print("CapacitorStatusBar: getSafeAreaInsets - top=\(safeAreaInsets.top), bottom=\(homeIndicatorHeight) (raw: \(safeAreaInsets.bottom)), left=\(safeAreaInsets.left), right=\(safeAreaInsets.right)")
            } else {
                print("CapacitorStatusBar: getSafeAreaInsets - Unable to get window, returning zero insets")
            }

            completion(insets)
        }
    }

    // MARK: - Navigation Bar (Home Indicator)

    /// Shared flag read by the swizzled `prefersHomeIndicatorAutoHidden` override.
    static var homeIndicatorHidden = false

    @objc public func showNavigationBar(animated: Bool, completion: (() -> Void)? = nil) {
        DispatchQueue.main.async {
            defer { completion?() }

            guard let windowScene = UIApplication.shared.connectedScenes.first as? UIWindowScene,
                  let window = windowScene.windows.first,
                  let rootVC = window.rootViewController else {
                print("CapacitorStatusBar: showNavigationBar - Unable to get root view controller")
                return
            }

            CapacitorStatusBar.homeIndicatorHidden = false
            CapacitorStatusBar.swizzleHomeIndicatorIfNeeded()
            rootVC.setNeedsUpdateOfHomeIndicatorAutoHidden()

            print("CapacitorStatusBar: showNavigationBar - animated=\(animated)")
            self.updateWebViewLayout()
            DispatchQueue.main.asyncAfter(deadline: .now() + 0.1) {
                self.updateWebViewLayout()
            }
        }
    }

    @objc public func hideNavigationBar(animation: String, completion: (() -> Void)? = nil) {
        DispatchQueue.main.async {
            defer { completion?() }

            guard let windowScene = UIApplication.shared.connectedScenes.first as? UIWindowScene,
                  let window = windowScene.windows.first,
                  let rootVC = window.rootViewController else {
                print("CapacitorStatusBar: hideNavigationBar - Unable to get root view controller")
                return
            }

            let animationType = animation.lowercased()
            CapacitorStatusBar.homeIndicatorHidden = true
            CapacitorStatusBar.swizzleHomeIndicatorIfNeeded()
            rootVC.setNeedsUpdateOfHomeIndicatorAutoHidden()

            print("CapacitorStatusBar: hideNavigationBar - animation=\(animationType)")
            self.updateWebViewLayout()
            DispatchQueue.main.asyncAfter(deadline: .now() + 0.1) {
                self.updateWebViewLayout()
            }
        }
    }

    // MARK: - Swizzling

    private static var hasSwizzledHomeIndicator = false
    private static var hasSwizzledStatusBarStyle = false
    private static var hasSwizzledStatusBarVisibility = false

    /// Current status bar style, read by the swizzled `preferredStatusBarStyle` override.
    static var currentStatusBarStyle: UIStatusBarStyle = .default

    /// Current status bar hidden state, read by the swizzled `prefersStatusBarHidden` override.
    static var currentStatusBarHidden = false

    /// Swizzle `preferredStatusBarStyle` so we can control status bar appearance
    /// without using private KVC APIs. Requires UIViewControllerBasedStatusBarAppearance = YES (default).
    static func swizzleStatusBarStyleIfNeeded() {
        guard !hasSwizzledStatusBarStyle else { return }
        hasSwizzledStatusBarStyle = true

        guard let windowScene = UIApplication.shared.connectedScenes.first as? UIWindowScene,
              let window = windowScene.windows.first,
              let rootVC = window.rootViewController else { return }

        let vcClass: AnyClass = type(of: rootVC)
        let originalSelector = #selector(getter: UIViewController.preferredStatusBarStyle)
        let swizzledSelector = #selector(UIViewController.capsb_preferredStatusBarStyle)

        guard let originalMethod = class_getInstanceMethod(vcClass, originalSelector),
              let swizzledMethod = class_getInstanceMethod(UIViewController.self, swizzledSelector) else {
            print("CapacitorStatusBar: Failed to swizzle preferredStatusBarStyle")
            return
        }

        method_exchangeImplementations(originalMethod, swizzledMethod)
        print("CapacitorStatusBar: Swizzled preferredStatusBarStyle on \(vcClass)")
    }

    /// Swizzle `prefersStatusBarHidden` so we can control status bar visibility
    /// without using private KVC APIs. Requires UIViewControllerBasedStatusBarAppearance = YES (default).
    static func swizzleStatusBarVisibilityIfNeeded() {
        guard !hasSwizzledStatusBarVisibility else { return }
        hasSwizzledStatusBarVisibility = true

        guard let windowScene = UIApplication.shared.connectedScenes.first as? UIWindowScene,
              let window = windowScene.windows.first,
              let rootVC = window.rootViewController else { return }

        let vcClass: AnyClass = type(of: rootVC)
        let originalSelector = #selector(getter: UIViewController.prefersStatusBarHidden)
        let swizzledSelector = #selector(UIViewController.capsb_prefersStatusBarHidden)

        guard let originalMethod = class_getInstanceMethod(vcClass, originalSelector),
              let swizzledMethod = class_getInstanceMethod(UIViewController.self, swizzledSelector) else {
            print("CapacitorStatusBar: Failed to swizzle prefersStatusBarHidden")
            return
        }

        method_exchangeImplementations(originalMethod, swizzledMethod)
        print("CapacitorStatusBar: Swizzled prefersStatusBarHidden on \(vcClass)")
    }

    /// Swizzle `prefersHomeIndicatorAutoHidden` on the root view controller so we
    /// can control the home indicator visibility from the plugin.
    static func swizzleHomeIndicatorIfNeeded() {
        guard !hasSwizzledHomeIndicator else { return }
        hasSwizzledHomeIndicator = true

        guard let windowScene = UIApplication.shared.connectedScenes.first as? UIWindowScene,
              let window = windowScene.windows.first,
              let rootVC = window.rootViewController else { return }

        let vcClass: AnyClass = type(of: rootVC)
        let originalSelector = #selector(getter: UIViewController.prefersHomeIndicatorAutoHidden)
        let swizzledSelector = #selector(UIViewController.capsb_prefersHomeIndicatorAutoHidden)

        guard let originalMethod = class_getInstanceMethod(vcClass, originalSelector),
              let swizzledMethod = class_getInstanceMethod(UIViewController.self, swizzledSelector) else {
            print("CapacitorStatusBar: Failed to swizzle prefersHomeIndicatorAutoHidden")
            return
        }

        method_exchangeImplementations(originalMethod, swizzledMethod)
        print("CapacitorStatusBar: Swizzled prefersHomeIndicatorAutoHidden on \(vcClass)")
    }

    /// Makes the status bar background view transparent
    private func makeStatusBarBackgroundTransparent() {
        guard let windowScene = UIApplication.shared.connectedScenes.first as? UIWindowScene,
              let window = windowScene.windows.first,
              let statusBarView = window.viewWithTag(CapacitorStatusBar.statusBarViewTag) else {
            return
        }

        statusBarView.backgroundColor = .clear
        print("CapacitorStatusBar: Made background transparent")
    }

    /// Updates WebView frame to respect overlay mode behavior.
    /// - overlay mode true: content extends edge-to-edge (no reserved top/bottom)
    /// - overlay mode false: content is inset away from status/home-indicator areas
    private func updateWebViewLayout() {
        guard let webView = self.webView else {
            print("CapacitorStatusBar: updateWebViewLayout - WebView unavailable")
            return
        }

        guard let window = webView.window
                ?? (UIApplication.shared.connectedScenes.first as? UIWindowScene)?
                    .windows.first(where: { $0.isKeyWindow })
                ?? (UIApplication.shared.connectedScenes.first as? UIWindowScene)?.windows.first else {
            print("CapacitorStatusBar: updateWebViewLayout - Unable to resolve window")
            return
        }

        let safeAreaInsets = window.safeAreaInsets
        let topInset: CGFloat = isOverlayMode ? 0 : sanitizedInsetValue(safeAreaInsets.top)
        let bottomInset: CGFloat = isOverlayMode ? 0 : sanitizedInsetValue(safeAreaInsets.bottom)

        var frame = window.bounds
        frame.origin.y = topInset
        frame.size.height = max(0, frame.size.height - topInset - bottomInset)

        if webView.frame != frame {
            webView.frame = frame
            print("CapacitorStatusBar: updateWebViewLayout - topInset=\(topInset), bottomInset=\(bottomInset), frame=\(frame)")
        }
    }

    /// Restores the status bar background view to its original color
    private func restoreStatusBarBackgroundColor() {
        guard let windowScene = UIApplication.shared.connectedScenes.first as? UIWindowScene,
              let window = windowScene.windows.first,
              let statusBarManager = windowScene.statusBarManager else {
            return
        }

        // Only restore if we have a stored color
        if let color = self.currentBackgroundColor {
            self.updateStatusBarBackgroundView(in: window,
                                               height: statusBarManager.statusBarFrame.height,
                                               color: color)
            print("CapacitorStatusBar: Restored background color: \(color)")
        }
    }

    // MARK: - Helper Methods

    private func colorFromHex(_ hex: String) -> UIColor? {
        var hexSanitized = hex.trimmingCharacters(in: .whitespacesAndNewlines)
        hexSanitized = hexSanitized.replacingOccurrences(of: "#", with: "")

        var rgb: UInt64 = 0
        guard Scanner(string: hexSanitized).scanHexInt64(&rgb) else { return nil }

        let length = hexSanitized.count
        let r, g, b, a: CGFloat

        if length == 6 {
            r = CGFloat((rgb & 0xFF0000) >> 16) / 255.0
            g = CGFloat((rgb & 0x00FF00) >> 8) / 255.0
            b = CGFloat(rgb & 0x0000FF) / 255.0
            a = 1.0
        } else if length == 8 {
            r = CGFloat((rgb & 0xFF000000) >> 24) / 255.0
            g = CGFloat((rgb & 0x00FF0000) >> 16) / 255.0
            b = CGFloat((rgb & 0x0000FF00) >> 8) / 255.0
            a = CGFloat(rgb & 0x000000FF) / 255.0
        } else {
            return nil
        }

        return UIColor(red: r, green: g, blue: b, alpha: a)
    }

    private func getColorBrightness(_ color: UIColor) -> CGFloat {
        var red: CGFloat = 0
        var green: CGFloat = 0
        var blue: CGFloat = 0
        var alpha: CGFloat = 0

        color.getRed(&red, green: &green, blue: &blue, alpha: &alpha)

        // Calculate relative luminance using the formula for sRGB
        return (0.299 * red + 0.587 * green + 0.114 * blue)
    }

    /// Guard against non-finite/negative inset values before bridging to JS.
    private func sanitizedInsetValue(_ value: CGFloat) -> CGFloat {
        guard value.isFinite else { return 0 }
        return max(0, value)
    }

    private func isSystemInDarkMode() -> Bool {
        guard let windowScene = UIApplication.shared.connectedScenes.first as? UIWindowScene,
              let window = windowScene.windows.first else {
            return false
        }
        return window.traitCollection.userInterfaceStyle == .dark
    }
}

// MARK: - UIViewController extension for home indicator swizzling

extension UIViewController {
    @objc func capsb_preferredStatusBarStyle() -> UIStatusBarStyle {
        return CapacitorStatusBar.currentStatusBarStyle
    }

    @objc func capsb_prefersStatusBarHidden() -> Bool {
        return CapacitorStatusBar.currentStatusBarHidden
    }

    @objc func capsb_prefersHomeIndicatorAutoHidden() -> Bool {
        return CapacitorStatusBar.homeIndicatorHidden
    }
}
