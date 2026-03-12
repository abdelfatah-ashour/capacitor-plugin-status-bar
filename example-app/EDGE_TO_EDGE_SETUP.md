# Edge-to-Edge Display Setup Guide

## Overview

This example app demonstrates how to properly configure edge-to-edge display with the CAP StatusBar plugin. When using `setOverlaysWebView(true)`, your app will draw edge-to-edge, and you need to handle safe areas properly.

## Architecture

### Native Side (Java/Kotlin)
The plugin handles:
- ✅ Setting up edge-to-edge mode (`WindowCompat.setDecorFitsSystemWindows(window, false)`)
- ✅ Creating colored overlay views for status bar and navigation bar
- ✅ Passing window insets to child views
- ✅ Handling IME (keyboard) insets

### Web Side (CSS/HTML)
Your app needs to handle:
- ✅ Adding safe area padding using CSS
- ✅ Using `viewport-fit=cover` in the viewport meta tag
- ✅ Respecting `env(safe-area-inset-*)` CSS variables

## Configuration

### 1. Viewport Meta Tag (index.html)
```html
<meta name="viewport" content="viewport-fit=cover, width=device-width, initial-scale=1.0, minimum-scale=1.0, maximum-scale=1.0, user-scalable=no" />
```
The `viewport-fit=cover` is **required** for safe area insets to work.

### 2. Global CSS (global.scss)
The safe area CSS has been added to `global.scss`. This ensures:
- Content doesn't overlap with status bar
- Content doesn't overlap with navigation bar
- Headers respect the status bar area
- Footers respect the navigation bar area

### 3. Initialize in App Component (app.component.ts)
```typescript
ngOnInit(): void {
  // Configure keyboard to resize body (prevents grey bar on Android 15+)
  Keyboard.setResizeMode({mode : KeyboardResize.Body});

  // Enable edge-to-edge mode
  CapacitorStatusBar.setOverlaysWebView({value : true});
}
```

## How It Works

1. **Java overlays** create colored backgrounds for status/navigation bar areas
2. **CSS safe areas** add padding to prevent content from being obscured by the overlays
3. **Window insets** propagate from native to web, making `env(safe-area-inset-*)` available

## Testing

1. Build and run the app on Android 15+ (API 35+)
2. Try different status bar styles (Light, Dark, Custom)
3. Open keyboard to verify no grey bar appears
4. Toggle "Overlays WebView" to see the difference
5. Click "Get Safe Area Insets" to see current values

## Expected Results

- ✅ Status bar has colored background
- ✅ Navigation bar has colored background
- ✅ Content doesn't overlap with system bars
- ✅ No grey bar above keyboard when typing
- ✅ Smooth transitions when keyboard appears/disappears

## Troubleshooting

### Content overlaps with status bar
- Check that `global.scss` has safe area CSS
- Verify `viewport-fit=cover` is in `index.html`
- Check that `setOverlaysWebView(true)` is called

### Grey bar above keyboard
- Ensure `Keyboard.setResizeMode({mode: KeyboardResize.Body})` is set
- Check that window insets are propagating (not consumed)

### Overlays not visible
- Check logcat for "ensureStatusBarOverlay" and "ensureNavBarOverlay" messages
- Verify insets are not being consumed with `WindowInsetsCompat.CONSUMED`
- Check that overlay views are receiving insets

