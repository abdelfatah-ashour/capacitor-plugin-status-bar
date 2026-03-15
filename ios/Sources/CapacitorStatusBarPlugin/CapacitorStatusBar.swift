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

    @objc public func applyDefaultStyle() {
        DispatchQueue.main.async {
            let isDarkMode = self.isSystemInDarkMode()
            let style = isDarkMode ? "DARK" : "LIGHT"
            print("CapacitorStatusBar: Applying default style based on system theme - isDarkMode=\(isDarkMode), style=\(style)")
            self.setStyle(style: style, colorHex: nil)
        }
    }

    @objc public func setStyle(style: String, colorHex: String?) {
        DispatchQueue.main.async {
            guard let windowScene = UIApplication.shared.connectedScenes.first as? UIWindowScene else { return }
            guard let window = windowScene.windows.first else { return }
            guard let statusBarManager = windowScene.statusBarManager else { return }

            let upperStyle = style.uppercased()
            var backgroundColor: UIColor?
            var statusBarStyle: UIStatusBarStyle = .default

            // Determine the status bar style and background color
            if upperStyle == "LIGHT" {
                // Light style: light background with dark content
                statusBarStyle = .darkContent
                backgroundColor = .white
            } else if upperStyle == "DARK" {
                // Dark style: dark background with light content
                statusBarStyle = .lightContent
                backgroundColor = .black
            } else if upperStyle == "CUSTOM" {
                // Custom style: use provided color and determine content style based on brightness
                if let colorHex = colorHex, let color = self.colorFromHex(colorHex) {
                    backgroundColor = color
                    let brightness = self.getColorBrightness(color)
                    // If background is light, use dark content; if dark, use light content
                    statusBarStyle = brightness > 0.5 ? .darkContent : .lightContent
                } else {
                    // No color provided, use system default
                    statusBarStyle = .default
                    backgroundColor = nil
                }
            } else {
                // Default: use system default
                statusBarStyle = .default
                backgroundColor = nil
            }

            // Set the status bar style using KVC to avoid deprecation warnings
            UIApplication.shared.setValue(statusBarStyle.rawValue, forKey: "statusBarStyle")

            // Store the background color for later restoration
            self.currentBackgroundColor = backgroundColor

            // Skip background color update when overlays web view is active
            if self.isOverlayMode {
                print("CapacitorStatusBar: setStyle - overlay mode active, skipping background color")
            } else {
                // Create or update the status bar background view
                self.updateStatusBarBackgroundView(in: window,
                                                   height: statusBarManager.statusBarFrame.height,
                                                   color: backgroundColor)
            }

            print("CapacitorStatusBar: setStyle - style=\(upperStyle), backgroundColor=\(String(describing: backgroundColor)), statusBarStyle=\(statusBarStyle)")
        }
    }

    @objc public func show(animated: Bool) {
        DispatchQueue.main.async {
            // Note: Status bar visibility is controlled through view controllers in modern iOS.
            // This plugin requires UIViewControllerBasedStatusBarAppearance to be set to NO
            // in the app's Info.plist for programmatic show/hide to work.

            // Log current status bar state via status bar manager
            if let windowScene = UIApplication.shared.connectedScenes.first as? UIWindowScene,
               let statusBarManager = windowScene.statusBarManager {
                print("CapacitorStatusBar: show() - Current hidden state: \(statusBarManager.isStatusBarHidden)")
            }

            // Set visibility using the application-level API
            // Note: This requires UIViewControllerBasedStatusBarAppearance = NO
            self.setStatusBarVisibility(hidden: false, animated: animated)

            // Restore the background view color when showing (unless overlay mode is active)
            if !self.isOverlayMode {
                self.restoreStatusBarBackgroundColor()
            }
        }
    }

    @objc public func hide(animation: String) {
        DispatchQueue.main.async {
            let animationType = animation.lowercased()

            // Log current status bar state via status bar manager
            if let windowScene = UIApplication.shared.connectedScenes.first as? UIWindowScene,
               let statusBarManager = windowScene.statusBarManager {
                print("CapacitorStatusBar: hide() - animation=\(animationType), Current hidden state: \(statusBarManager.isStatusBarHidden)")
            }

            if animationType == "fade" {
                // Fade mode: Make background transparent without removing status bar
                print("CapacitorStatusBar: hide() - fade mode: making background transparent")
                self.makeStatusBarBackgroundTransparent()
            } else if animationType == "slide" {
                // Slide mode: Hide the status bar completely (current behavior)
                print("CapacitorStatusBar: hide() - slide mode: hiding bars completely")
                // Note: Status bar visibility is controlled through view controllers in modern iOS.
                // This plugin requires UIViewControllerBasedStatusBarAppearance to be set to NO
                // in the app's Info.plist for programmatic show/hide to work.
                self.setStatusBarVisibility(hidden: true, animated: true)
                // Also make the background view transparent when hiding
                self.makeStatusBarBackgroundTransparent()
            } else {
                print("CapacitorStatusBar: hide() - unknown animation type '\(animationType)', defaulting to slide")
                self.setStatusBarVisibility(hidden: true, animated: true)
                self.makeStatusBarBackgroundTransparent()
            }
        }
    }

    // MARK: - Private Methods

    /// Sets the status bar visibility.
    /// - Parameters:
    ///   - hidden: Whether the status bar should be hidden
    ///   - animated: Whether the change should be animated
    private func setStatusBarVisibility(hidden: Bool, animated: Bool) {
        // Use KVC to set status bar state without triggering deprecation warnings
        // This approach is necessary when UIViewControllerBasedStatusBarAppearance is NO
        UIApplication.shared.setValue(hidden, forKey: "statusBarHidden")
    }

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

    @objc public func setOverlaysWebView(value: Bool) {
        DispatchQueue.main.async {
            guard let windowScene = UIApplication.shared.connectedScenes.first as? UIWindowScene,
                  let window = windowScene.windows.first,
                  let statusBarManager = windowScene.statusBarManager else {
                print("CapacitorStatusBar: setOverlaysWebView - Unable to get window or status bar manager")
                return
            }

            self.isOverlayMode = value

            if value {
                // Overlay mode: make the status bar background transparent so web content shows through
                let statusBarView = window.viewWithTag(CapacitorStatusBar.statusBarViewTag)
                statusBarView?.backgroundColor = .clear
                print("CapacitorStatusBar: setOverlaysWebView(true) - content extends behind status bar")
            } else {
                // Non-overlay mode: restore the status bar background from the current style
                if let color = self.currentBackgroundColor {
                    self.updateStatusBarBackgroundView(in: window,
                                                       height: statusBarManager.statusBarFrame.height,
                                                       color: color)
                    print("CapacitorStatusBar: setOverlaysWebView(false) - restored background color")
                } else {
                    // No style was set; apply default style based on system theme
                    self.applyDefaultStyle()
                    print("CapacitorStatusBar: setOverlaysWebView(false) - applied default style from config")
                }
            }
        }
    }

    @objc public func setBackground(colorHex: String?) {
        DispatchQueue.main.async {
            guard let colorHex = colorHex, let color = self.colorFromHex(colorHex) else {
                print("CapacitorStatusBar: setBackground - Invalid color or nil")
                return
            }

            guard let windowScene = UIApplication.shared.connectedScenes.first as? UIWindowScene,
                  let window = windowScene.windows.first else {
                print("CapacitorStatusBar: setBackground - Unable to get window")
                return
            }

            window.backgroundColor = color
            print("CapacitorStatusBar: setBackground - Set window background to \(colorHex)")
        }
    }

    @objc public func getSafeAreaInsets(completion: @escaping ([String: CGFloat]) -> Void) {
        DispatchQueue.main.async {
            var insets: [String: CGFloat] = ["top": 0, "bottom": 0, "left": 0, "right": 0]

            if let windowScene = UIApplication.shared.connectedScenes.first as? UIWindowScene,
               let window = windowScene.windows.first {
                // Use statusBarManager for accurate status bar height
                let statusBarHeight = windowScene.statusBarManager?.statusBarFrame.height ?? 0
                let safeAreaInsets = window.safeAreaInsets

                // top: status bar height specifically
                insets["top"] = statusBarHeight
                // bottom: home indicator / navigation bar area
                insets["bottom"] = safeAreaInsets.bottom
                // left/right: safe area for landscape / display cutouts
                insets["left"] = safeAreaInsets.left
                insets["right"] = safeAreaInsets.right

                print("CapacitorStatusBar: getSafeAreaInsets - top=\(statusBarHeight), bottom=\(safeAreaInsets.bottom), left=\(safeAreaInsets.left), right=\(safeAreaInsets.right)")
            } else {
                print("CapacitorStatusBar: getSafeAreaInsets - Unable to get window, returning zero insets")
            }

            completion(insets)
        }
    }

    // MARK: - Navigation Bar (Home Indicator)

    /// Shared flag read by the swizzled `prefersHomeIndicatorAutoHidden` override.
    static var homeIndicatorHidden = false

    @objc public func showNavigationBar(animated: Bool) {
        DispatchQueue.main.async {
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
        }
    }

    @objc public func hideNavigationBar(animation: String) {
        DispatchQueue.main.async {
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
        }
    }

    // MARK: - Home Indicator Swizzling

    private static var hasSwizzled = false

    /// Swizzle `prefersHomeIndicatorAutoHidden` on the root view controller so we
    /// can control the home indicator visibility from the plugin.
    static func swizzleHomeIndicatorIfNeeded() {
        guard !hasSwizzled else { return }
        hasSwizzled = true

        guard let windowScene = UIApplication.shared.connectedScenes.first as? UIWindowScene,
              let window = windowScene.windows.first,
              let rootVC = window.rootViewController else {
            return
        }

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
    @objc func capsb_prefersHomeIndicatorAutoHidden() -> Bool {
        return CapacitorStatusBar.homeIndicatorHidden
    }
}
