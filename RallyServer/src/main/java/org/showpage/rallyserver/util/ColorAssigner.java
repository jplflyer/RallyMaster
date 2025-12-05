package org.showpage.rallyserver.util;

/**
 * Utility for automatically assigning distinct colors to combinations.
 * Uses a palette of visually distinct colors that work well on maps.
 */
public class ColorAssigner {

    /**
     * Predefined palette of distinct, map-friendly colors.
     * These colors are chosen to be visually distinct and work well on map backgrounds.
     */
    private static final String[] COLOR_PALETTE = {
        "#FF0000",  // Red
        "#0000FF",  // Blue
        "#00FF00",  // Lime
        "#FF00FF",  // Magenta
        "#00FFFF",  // Cyan
        "#FF8C00",  // Dark Orange
        "#8B00FF",  // Violet
        "#FFD700",  // Gold
        "#00CED1",  // Dark Turquoise
        "#FF1493",  // Deep Pink
        "#32CD32",  // Lime Green
        "#FF4500",  // Orange Red
        "#4B0082",  // Indigo
        "#00FF7F",  // Spring Green
        "#DC143C",  // Crimson
        "#1E90FF",  // Dodger Blue
        "#FF69B4",  // Hot Pink
        "#00FA9A",  // Medium Spring Green
        "#FF6347",  // Tomato
        "#4169E1",  // Royal Blue
        "#ADFF2F",  // Green Yellow
        "#FF8C69",  // Salmon
        "#8A2BE2",  // Blue Violet
        "#7FFF00",  // Chartreuse
    };

    /**
     * Get a color for a given index. If there are more combinations than colors,
     * this will cycle through the palette with slight variations.
     *
     * @param index Zero-based index of the combination
     * @return Hex color code (e.g., "#FF0000")
     */
    public static String getColorForIndex(int index) {
        if (index < 0) {
            index = 0;
        }

        // For the first cycle, use colors as-is
        if (index < COLOR_PALETTE.length) {
            return COLOR_PALETTE[index];
        }

        // For subsequent cycles, we could add subtle variations
        // For now, just cycle through the palette
        int paletteIndex = index % COLOR_PALETTE.length;
        return COLOR_PALETTE[paletteIndex];
    }

    /**
     * Get the total number of base colors in the palette.
     *
     * @return Number of distinct colors available
     */
    public static int getPaletteSize() {
        return COLOR_PALETTE.length;
    }
}
