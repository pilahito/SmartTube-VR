import com.liskovsoft.smartyoutubetv2.tv.ui.playback.cinema.VirtualScreen;

public class VirtualScreenTest {
    static void check(boolean ok, String msg) { if (!ok) throw new AssertionError(msg); }
    public static void main(String[] args) {
        check(VirtualScreen.modeNames(true).length == VirtualScreen.MODE_COUNT, "names ES");
        check(VirtualScreen.modeNames(false).length == VirtualScreen.MODE_COUNT, "names EN");
        check("Ajustar".equals(VirtualScreen.modeNames(true)[VirtualScreen.FIT]), "Ajustar");
        check(VirtualScreen.resizeMode(VirtualScreen.FILL) == VirtualScreen.RESIZE_ZOOM, "fill zooms");
        for (int m : new int[]{VirtualScreen.FIT, VirtualScreen.R16_9, VirtualScreen.R21_9, VirtualScreen.R4_3})
            check(VirtualScreen.resizeMode(m) == VirtualScreen.RESIZE_FIT, "letterbox " + m);
        check(VirtualScreen.aspect(VirtualScreen.FIT) == 0f && VirtualScreen.aspect(VirtualScreen.FILL) == 0f, "auto aspect");
        check(VirtualScreen.aspect(VirtualScreen.R21_9) == 2.33f && VirtualScreen.aspect(VirtualScreen.R4_3) == 1.33f
                && VirtualScreen.aspect(VirtualScreen.R16_9) == 1.77f, "forced aspects");
        check(VirtualScreen.clampMode(99) == VirtualScreen.FIT && VirtualScreen.clampMode(-1) == VirtualScreen.FIT, "bad saved mode");
        check(VirtualScreen.factor(100, 2) == 1f, "max factor");
        check(VirtualScreen.factor(50, 5) == .35f, "min factor");
        check(VirtualScreen.factor(72, 3) > VirtualScreen.factor(72, 4), "farther is smaller");
        int[] r = VirtualScreen.rect(1000, 600, 50, 3);
        check(r[0] == 250 && r[1] == 150 && r[2] == 750 && r[3] == 450, "centred rect");
        check(VirtualScreen.curveDepth(400, 2) > VirtualScreen.curveDepth(400, 5), "closer bends more");
        // 32:9 ultrawide and the curvature slider of the "Pantalla virtual" panel
        check(VirtualScreen.MODE_COUNT == 6 && "32:9".equals(VirtualScreen.modeNames(false)[VirtualScreen.R32_9]), "32:9 mode");
        check(VirtualScreen.aspect(VirtualScreen.R32_9) == 3.56f && VirtualScreen.resizeMode(VirtualScreen.R32_9) == VirtualScreen.RESIZE_FIT, "32:9 letterbox");
        check(VirtualScreen.clampCurvature(0) == VirtualScreen.CURVATURE_MIN && VirtualScreen.clampCurvature(999) == VirtualScreen.CURVATURE_MAX, "curvature clamp");
        check(VirtualScreen.curveDepth(400, 3, 100) == VirtualScreen.curveDepth(400, 3), "100% is the original bulge");
        check(VirtualScreen.curveDepth(400, 3, 150) > VirtualScreen.curveDepth(400, 3, 50), "more curvature bends more");
        for (int i = 0; i < VirtualScreen.PRESET_COUNT; i++) {
            int[] p = VirtualScreen.preset(i);
            check(p.length == 6 && p[5] == VirtualScreen.clampHeight(p[5]) && p[0] == VirtualScreen.clampSize(p[0]) && p[1] == VirtualScreen.clampDistance(p[1])
                    && p[3] == VirtualScreen.clampCurvature(p[3]) && p[4] == VirtualScreen.clampMode(p[4]), "preset in range " + i);
            check((p[0] - 50) % 5 == 0, "preset size matches the 5% slider step " + i);
        }
        check(VirtualScreen.presetNames(true).length == VirtualScreen.PRESET_COUNT && VirtualScreen.presetNames(false).length == VirtualScreen.PRESET_COUNT, "preset names");
        // "Altura": 0 keeps the screen centred; +100 moves it to the top and -100 to the bottom of the free space.
        int[] mid = VirtualScreen.rect(1000, 600, 50, 3, 0), up = VirtualScreen.rect(1000, 600, 50, 3, 100), down = VirtualScreen.rect(1000, 600, 50, 3, -100);
        check(mid[1] == 150 && up[1] == 0 && down[3] == 600, "height moves the screen inside the panel");
        check(up[3] - up[1] == mid[3] - mid[1] && up[0] == mid[0], "height keeps size and horizontal position");
        check(VirtualScreen.clampHeight(500) == VirtualScreen.HEIGHT_MAX && VirtualScreen.clampHeight(-500) == VirtualScreen.HEIGHT_MIN, "height clamp");
        System.out.println("VirtualScreenTest OK");
    }
}
