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
        System.out.println("VirtualScreenTest OK");
    }
}
