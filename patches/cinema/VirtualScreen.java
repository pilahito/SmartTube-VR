package com.liskovsoft.smartyoutubetv2.tv.ui.playback.cinema;

/**
 * Pure logic for the "Pantalla virtual" settings (no Android imports, unit-tested on the JVM).
 * SmartTube runs as a flat 2D panel on Quest: this only resizes/reframes the video inside that
 * panel. It cannot move the Horizon OS window or bend the real video surface.
 */
public final class VirtualScreen {
    // Format modes. Saved by index: append new ones at the end.
    public static final int FIT = 0, FILL = 1, R16_9 = 2, R21_9 = 3, R4_3 = 4, MODE_COUNT = 5;
    // Same values as SmartTube's AspectRatioFrameLayout / PlayerConstants.
    public static final int RESIZE_FIT = 0, RESIZE_ZOOM = 4;
    public static final float ASPECT_DEFAULT = 0f, ASPECT_16_9 = 1.77f, ASPECT_21_9 = 2.33f, ASPECT_4_3 = 1.33f;
    public static final int[] SIZES = {50, 60, 72, 82, 90, 100};
    public static final int[] DISTANCES = {2, 3, 4, 5};
    private static final String[] MODES_ES = {"Ajustar", "Rellenar (recorta bordes)", "16:9", "21:9", "4:3"};
    private static final String[] MODES_EN = {"Fit", "Fill (crops edges)", "16:9", "21:9", "4:3"};

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
            default: return ASPECT_DEFAULT;
        }
    }
    /** Fraction of the panel used by the screen: size scaled by a 3 m reference distance. */
    public static float factor(int size, int distance) {
        return Math.max(.35f, Math.min(1f, clampSize(size) / 100f * 3f / clampDistance(distance)));
    }
    /** Screen rectangle {left, top, right, bottom} centred in a panel of w x h. */
    public static int[] rect(int w, int h, int size, int distance) {
        float f = factor(size, distance);
        int sw = Math.round(w * f), sh = Math.round(h * f);
        int l = (w - sw) / 2, t = (h - sh) / 2;
        return new int[]{l, t, l + sw, t + sh};
    }
    /** Vertical bulge (px) of the decorative curved frame: grows when the screen is closer. */
    public static float curveDepth(int screenHeight, int distance) {
        return screenHeight * .06f * 3f / clampDistance(distance);
    }
}
