import Foundation
import UIKit
import Capacitor

@objc public class CAPStatusBar: NSObject {
    // Tag to identify the status bar background view
    private static let statusBarViewTag = 38482458
    // Store the current background color to restore when showing
    private var currentBackgroundColor: UIColor?

    @objc public func applyDefaultStyle() {
        DispatchQueue.main.async {
            let isDarkMode = self.isSystemInDarkMode()
            let style = isDarkMode ? "DARK" : "LIGHT"
            print("CAPStatusBar: Applying default style based on system theme - isDarkMode=\(isDarkMode), style=\(style)")
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

            // Create or update the status bar background view
            self.updateStatusBarBackgroundView(in: window,
                                               height: statusBarManager.statusBarFrame.height,
                                               color: backgroundColor)

            print("CAPStatusBar: setStyle - style=\(upperStyle), backgroundColor=\(String(describing: backgroundColor)), statusBarStyle=\(statusBarStyle)")
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
                print("CAPStatusBar: show() - Current hidden state: \(statusBarManager.isStatusBarHidden)")
            }

            // Set visibility using the application-level API
            // Note: This requires UIViewControllerBasedStatusBarAppearance = NO
            self.setStatusBarVisibility(hidden: false, animated: animated)

            // Restore the background view color when showing
            self.restoreStatusBarBackgroundColor()
        }
    }

    @objc public func hide(animated: Bool) {
        DispatchQueue.main.async {
            // Note: Status bar visibility is controlled through view controllers in modern iOS.
            // This plugin requires UIViewControllerBasedStatusBarAppearance to be set to NO
            // in the app's Info.plist for programmatic show/hide to work.

            // Log current status bar state via status bar manager
            if let windowScene = UIApplication.shared.connectedScenes.first as? UIWindowScene,
               let statusBarManager = windowScene.statusBarManager {
                print("CAPStatusBar: hide() - Current hidden state: \(statusBarManager.isStatusBarHidden)")
            }

            // Set visibility using the application-level API
            // Note: This requires UIViewControllerBasedStatusBarAppearance = NO
            self.setStatusBarVisibility(hidden: true, animated: animated)

            // Make the background view transparent when hiding
            self.makeStatusBarBackgroundTransparent()
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
        let existingView = window.viewWithTag(CAPStatusBar.statusBarViewTag)

        if let color = color {
            // Create or update the status bar background view
            let statusBarView: UIView

            if let existing = existingView {
                statusBarView = existing
            } else {
                statusBarView = UIView(frame: CGRect(x: 0, y: 0, width: window.bounds.width, height: height))
                statusBarView.tag = CAPStatusBar.statusBarViewTag
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
        // No-op on iOS; Capacitor uses safe areas. Exposed for API parity.

    }

    @objc public func setBackground(colorHex: String?) {
        DispatchQueue.main.async {
            guard let colorHex = colorHex, let color = self.colorFromHex(colorHex) else {
                print("CAPStatusBar: setBackground - Invalid color or nil")
                return
            }

            guard let windowScene = UIApplication.shared.connectedScenes.first as? UIWindowScene,
                  let window = windowScene.windows.first else {
                print("CAPStatusBar: setBackground - Unable to get window")
                return
            }

            window.backgroundColor = color
            print("CAPStatusBar: setBackground - Set window background to \(colorHex)")
        }
    }

    @objc public func setStatusBarColor(colorHex: String?) {
        DispatchQueue.main.async {
            guard let colorHex = colorHex, let color = self.colorFromHex(colorHex) else {
                print("CAPStatusBar: setStatusBarColor - Invalid color or nil")
                return
            }

            guard let windowScene = UIApplication.shared.connectedScenes.first as? UIWindowScene,
                  let window = windowScene.windows.first,
                  let statusBarManager = windowScene.statusBarManager else {
                print("CAPStatusBar: setStatusBarColor - Unable to get window or status bar manager")
                return
            }

            // Store the current background color for restoration
            self.currentBackgroundColor = color

            // Update the status bar background view
            self.updateStatusBarBackgroundView(in: window,
                                               height: statusBarManager.statusBarFrame.height,
                                               color: color)

            // Determine and set the appropriate status bar style based on color brightness
            let brightness = self.getColorBrightness(color)
            let statusBarStyle: UIStatusBarStyle = brightness > 0.5 ? .darkContent : .lightContent

            if #available(iOS 13.0, *) {
                if let viewController = window.rootViewController {
                    viewController.overrideUserInterfaceStyle = brightness > 0.5 ? .light : .dark
                }
            }

            // Apply status bar style
            UIApplication.shared.statusBarStyle = statusBarStyle

            print("CAPStatusBar: setStatusBarColor - Set status bar color to \(colorHex), brightness: \(brightness), style: \(statusBarStyle)")
        }
    }

    @objc public func getSafeAreaInsets(completion: @escaping ([String: CGFloat]) -> Void) {
        DispatchQueue.main.async {
            var insets: [String: CGFloat] = ["top": 0, "bottom": 0, "left": 0, "right": 0]

            if let windowScene = UIApplication.shared.connectedScenes.first as? UIWindowScene,
               let window = windowScene.windows.first {
                let safeAreaInsets = window.safeAreaInsets
                insets["top"] = safeAreaInsets.top
                insets["bottom"] = safeAreaInsets.bottom
                insets["left"] = safeAreaInsets.left
                insets["right"] = safeAreaInsets.right

                print("CAPStatusBar: getSafeAreaInsets - top=\(safeAreaInsets.top), bottom=\(safeAreaInsets.bottom), left=\(safeAreaInsets.left), right=\(safeAreaInsets.right)")
            } else {
                print("CAPStatusBar: getSafeAreaInsets - Unable to get window, returning zero insets")
            }

            completion(insets)
        }
    }

    /// Makes the status bar background view transparent
    private func makeStatusBarBackgroundTransparent() {
        guard let windowScene = UIApplication.shared.connectedScenes.first as? UIWindowScene,
              let window = windowScene.windows.first,
              let statusBarView = window.viewWithTag(CAPStatusBar.statusBarViewTag) else {
            return
        }

        statusBarView.backgroundColor = .clear
        print("CAPStatusBar: Made background transparent")
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
            print("CAPStatusBar: Restored background color: \(color)")
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
