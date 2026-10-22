package com.liskovsoft.smartyoutubetv2.tv.ui.playback.cinema;

/**
 * Pure logic for the "Pantalla virtual" settings (no Android imports, unit-tested on the JVM).
 * SmartTube runs as a flat 2D panel on Quest: this only resizes/reframes the video inside that
 * panel. It cannot move the Horizon OS window or bend the real video surface.
 */
public final class VirtualScreen {
    // Format modes. Saved by index: append new ones at the end.
    public static final int FIT = 0, FILL = 1, R16_9 = 2, R21_9 = 3, R4_3 = 4, R32_9 = 5, MODE_COUNT = 6;
    // Same values as SmartTube's AspectRatioFrameLayout / PlayerConstants.
    public static final int RESIZE_FIT = 0, RESIZE_ZOOM = 4;
    public static final float ASPECT_DEFAULT = 0f, ASPECT_16_9 = 1.77f, ASPECT_21_9 = 2.33f, ASPECT_4_3 = 1.33f, ASPECT_32_9 = 3.56f;
    public static final int[] SIZES = {50, 60, 72, 82, 90, 100};
    public static final int[] DISTANCES = {2, 3, 4, 5};
    private static final String[] MODES_ES = {"Ajustar", "Rellenar (recorta bordes)", "16:9", "21:9", "4:3", "32:9"};
    private static final String[] MODES_EN = {"Fit", "Fill (crops edges)", "16:9", "21:9", "4:3", "32:9"};

    private VirtualScreen() {}
    public static String[] modeNames(boolean spanish) { return (spanish ? MODES_ES : MODES_EN).clone(); }
    public static int clampMode(int mode) { return mode < 0 || mode >= MODE_COUNT ? FIT : mode; }
    public static int clampSize(int size) { return Math.max(50, Math.min(100, size)); }
    public static int clampDistance(int d) { return Math.max(2, Math.min(5, d)); }
    /** SmartTube resize mode for a format: only "Rellenar" zooms, the rest letterbox. */
    public static int resizeMode(int mode) { return clampMode(mode) == FILL ? RESIZE_ZOOM : RESIZE_FIT; }
    /** Forced aspect for a format; 0 lets SmartTube use the video's own aspect. */
    public static float aspect(int mode) {
        switch (clampMode(mode)) {
            case R16_9: return ASPECT_16_9;
            case R21_9: return ASPECT_21_9;
            case R4_3: return ASPECT_4_3;
            case R32_9: return ASPECT_32_9;
            default: return ASPECT_DEFAULT;
        }
    }
    /** Fraction of the panel used by the screen: size scaled by a 3 m reference distance. */
    public static float factor(int size, int distance) {
        return Math.max(.35f, Math.min(1f, clampSize(size) / 100f * 3f / clampDistance(distance)));
    }
    /** Screen rectangle {left, top, right, bottom} centred in a panel of w x h. */
    public static int[] rect(int w, int h, int size, int distance) {
        return rect(w, h, size, distance, 0);
    }
    // "Altura": -100 (screen at the bottom of the free space) .. 0 (centred) .. 100 (at the top).
    public static final int HEIGHT_MIN = -100, HEIGHT_MAX = 100;
    public static int clampHeight(int h) { return Math.max(HEIGHT_MIN, Math.min(HEIGHT_MAX, h)); }
    /** Like {@link #rect(int, int, int, int)} but moved up or down inside the free space by {@code height}. */
    public static int[] rect(int w, int h, int size, int distance, int height) {
        float f = factor(size, distance);
        int sw = Math.round(w * f), sh = Math.round(h * f);
        int l = (w - sw) / 2, free = (h - sh) / 2;
        int t = free - Math.round(free * clampHeight(height) / 100f);
        return new int[]{l, t, l + sw, t + sh};
    }
    /** Vertical bulge (px) of the decorative curved frame: grows when the screen is closer. */
    public static float curveDepth(int screenHeight, int distance) {
        return screenHeight * .06f * 3f / clampDistance(distance);
    }
    // Curvature slider: percent of the default bulge (100 = the original look).
    public static final int CURVATURE_MIN = 20, CURVATURE_MAX = 150, CURVATURE_DEFAULT = 100;
    public static int clampCurvature(int percent) { return Math.max(CURVATURE_MIN, Math.min(CURVATURE_MAX, percent)); }
    public static float curveDepth(int screenHeight, int distance, int curvaturePercent) {
        return curveDepth(screenHeight, distance) * clampCurvature(curvaturePercent) / 100f;
    }
    // Presets of the "Pantalla virtual" panel: {size %, distance m, curved (0/1), curvature %, format mode, height}.
    // "Tumbado (techo)" puts a big screen at the top of the panel, for watching while lying down.
    public static final int PRESET_CINEMA = 0, PRESET_LYING = 1, PRESET_DESK = 2, PRESET_COUNT = 3;
    public static int[] preset(int which) {
        switch (which) {
            case PRESET_LYING: return new int[]{90, 2, 1, CURVATURE_DEFAULT, FIT, 100};
            case PRESET_DESK: return new int[]{80, 2, 0, CURVATURE_DEFAULT, FIT, 0};
            default: return new int[]{100, 3, 1, CURVATURE_DEFAULT, FIT, 0};
        }
    }
    private static final String[] PRESETS_ES = {"Cine", "Tumbado (techo)", "Escritorio"};
    private static final String[] PRESETS_EN = {"Cinema", "Lying down (ceiling)", "Desk"};
    public static String[] presetNames(boolean spanish) { return (spanish ? PRESETS_ES : PRESETS_EN).clone(); }
}
