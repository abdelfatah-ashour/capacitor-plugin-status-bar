// swift-tools-version: 5.9
import PackageDescription

let package = Package(
    name: "CapStatusBar",
    platforms: [.iOS(.v15)],
    products: [
        .library(
            name: "CapStatusBar",
            targets: ["CAPStatusBarPlugin"])
    ],
    dependencies: [
        .package(url: "https://github.com/ionic-team/capacitor-swift-pm.git", from: "8.0.0")
    ],
    targets: [
        .target(
            name: "CAPStatusBarPlugin",
            dependencies: [
                .product(name: "Capacitor", package: "capacitor-swift-pm"),
                .product(name: "Cordova", package: "capacitor-swift-pm")
            ],
            path: "ios/Sources/CAPStatusBarPlugin"),
        .testTarget(
            name: "CAPStatusBarPluginTests",
            dependencies: ["CAPStatusBarPlugin"],
            path: "ios/Tests/CAPStatusBarPluginTests")
    ]
)
