/**
 * Test class to verify Android HEX color parsing matches iOS behavior
 */
public class ColorParsingTest {

    public static void main(String[] args) {
        testColorParsing();
    }

    public static void testColorParsing() {
        // Test cases that should match iOS behavior
        String[] testColors = {
                "#FF0000", // Red (opaque)
                "#00FF00", // Green (opaque)
                "#0000FF", // Blue (opaque)
                "#FF000080", // Red (50% opacity)
                "#00FF0080", // Green (50% opacity)
                "#0000FF80", // Blue (50% opacity)
                "#FFFFFF", // White (opaque)
                "#000000", // Black (opaque)
                "#FFFFFF80", // White (50% opacity)
                "#00000080" // Black (50% opacity)
        };

        for (String color : testColors) {
            try {
                int parsed = parseHexColor(color);
                int alpha = Color.alpha(parsed);
                int red = Color.red(parsed);
                int green = Color.green(parsed);
                int blue = Color.blue(parsed);
                boolean isLight = isEffectiveLightColor(parsed);

                System.out.printf("%s -> ARGB(%d,%d,%d,%d) -> Light: %s%n",
                        color, alpha, red, green, blue, isLight);

            } catch (Exception e) {
                System.out.printf("%s -> ERROR: %s%n", color, e.getMessage());
            }
        }
    }

    // Copy of the improved Android methods for testing
    private static int parseHexColor(String hex) throws IllegalArgumentException {
        String hexSanitized = hex.trim().replaceFirst("^#", "");

        if (hexSanitized.length() != 6 && hexSanitized.length() != 8) {
            throw new IllegalArgumentException("Invalid hex color length: " + hexSanitized.length());
        }

        try {
            long rgb = Long.parseLong(hexSanitized, 16);

            if (hexSanitized.length() == 6) {
                // 6-digit format: #RRGGBB (opaque)
                return (int) (0xFF000000L | rgb);
            } else {
                // 8-digit format: #RRGGBBAA
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

    private static boolean isEffectiveLightColor(int color) {
        int alpha = Color.alpha(color);

        if (alpha == 255) {
            // Fully opaque - use standard luminance calculation
            return calculateLuminance(color) > 0.5;
        }

        // For transparent colors, calculate effective color when blended over white
        // background
        float alphaRatio = alpha / 255.0f;
        int r = Color.red(color);
        int g = Color.green(color);
        int b = Color.blue(color);

        // Blend with white background (255, 255, 255)
        int effectiveR = (int) (r * alphaRatio + 255 * (1 - alphaRatio));
        int effectiveG = (int) (g * alphaRatio + 255 * (1 - alphaRatio));
        int effectiveB = (int) (b * alphaRatio + 255 * (1 - alphaRatio));

        int effectiveColor = Color.rgb(effectiveR, effectiveG, effectiveB);
        return calculateLuminance(effectiveColor) > 0.5;
    }

    // Simple luminance calculation for testing
    private static double calculateLuminance(int color) {
        double red = Color.red(color) / 255.0;
        double green = Color.green(color) / 255.0;
        double blue = Color.blue(color) / 255.0;

        return (0.299 * red + 0.587 * green + 0.114 * blue);
    }
}