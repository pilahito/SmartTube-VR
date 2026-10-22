package com.liskovsoft.smartyoutubetv2.tv.ui.playback.cinema;

/**
 * Colors of the VR interface follow the scenery (docs/interfaz/adapta-oceano.jpg and adapta-cyberpunk.jpg):
 * same layout, different accent, border and progress-bar colors. Panels built after apply() use the new colors.
 */
final class QuestTheme {
    private QuestTheme() {}

    /** {accent, border, progress} for a CinemaGeometry scene index. */
    static int[] colors(int scene) {
        switch (scene) {
            case CinemaGeometry.BEACH: case CinemaGeometry.BEACH_TROPICAL:
                return new int[]{0xff27d9d1, 0x6627d9d1, 0xff27d9d1};
            case CinemaGeometry.BEACH_SUNSET:
                return new int[]{0xffff8a3d, 0x66ff8a3d, 0xffff8a3d};
            case CinemaGeometry.BEACH_NIGHT: case CinemaGeometry.SPACE:
                return new int[]{0xff5b8cff, 0x665b8cff, 0xff5b8cff};
            case CinemaGeometry.CYBER: case CinemaGeometry.RETRO: case CinemaGeometry.RGB:
                return new int[]{0xffc43cff, 0x80c43cff, 0xffe03333};
            case CinemaGeometry.MAGIC_FOREST:
                return new int[]{0xff9b6bff, 0x669b6bff, 0xff9b6bff};
            case CinemaGeometry.FOREST: case CinemaGeometry.MOUNTAINS:
                return new int[]{0xff3ddc84, 0x663ddc84, 0xff3ddc84};
            case CinemaGeometry.CINEMA:
                return new int[]{0xffd9a441, 0x66d9a441, 0xffe03333};
            case CinemaGeometry.LOUNGE_FUTURE:
                return new int[]{0xff2fd0ff, 0x662fd0ff, 0xff2fd0ff};
            default: // lounges
                return new int[]{0xffff9f43, 0x66ff9f43, 0xffe03333};
        }
    }

    /** Borders and the progress bar follow the scenery; sliders and selections stay blue, as in the mockups. */
    static void apply(int scene) {
        int[] c = colors(scene);
        QuestUi.border = c[1];
        QuestUi.progress = c[2];
    }
}
