import com.liskovsoft.smartyoutubetv2.tv.ui.home.HomeTiles;
import java.util.ArrayList;
import java.util.List;

public class HomeTilesTest {
    private static void check(boolean ok, String message) { if (!ok) throw new AssertionError(message); }
    public static void main(String[] args) {
        List<HomeTiles.Tile> tiles = new ArrayList<>();
        check(HomeTiles.add(tiles, new HomeTiles.Tile(HomeTiles.APP, HomeTiles.BRAVE, "Brave")), "Brave pin");
        check(!HomeTiles.add(tiles, new HomeTiles.Tile(HomeTiles.APP, HomeTiles.BRAVE, "Brave")), "Duplicate app accepted");
        check(HomeTiles.add(tiles, new HomeTiles.Tile(HomeTiles.WEB, "https://example.org/series", "Mis\tseries\n")), "Web pin");
        check(!HomeTiles.add(tiles, new HomeTiles.Tile(HomeTiles.WEB, "javascript:alert(1)", "x")), "Unsafe URL accepted");
        check(!HomeTiles.add(tiles, new HomeTiles.Tile(HomeTiles.APP, "not a package", "x")), "Invalid package accepted");

        List<HomeTiles.Tile> round = HomeTiles.decode(HomeTiles.encode(tiles));
        check(round.size() == 2 && round.get(1).label.equals("Mis series"), "Round trip lost data: " + HomeTiles.encode(tiles));
        check(HomeTiles.decode("garbage\napp\tbad pkg\tX\nweb\tftp://x.org\tY").isEmpty(), "Corrupt prefs not ignored");

        check(HomeTiles.move(round, 1, -1) == 0 && round.get(0).target.equals("https://example.org/series"), "Move left");
        check(HomeTiles.move(round, 0, -1) == 0, "Move past start");
        check(HomeTiles.move(round, 0, 1) == 1 && round.get(1).isApp() == false, "Move right");

        check("https://www.netflix.com".equals(HomeTiles.normalizeUrl(" www.netflix.com ")), "Scheme not added");
        check("http://192.168.0.10:8080/".equals(HomeTiles.normalizeUrl("http://192.168.0.10:8080/")), "LAN URL rejected");
        check(HomeTiles.normalizeUrl("file:///sdcard/x") == null, "file URL accepted");
        check(HomeTiles.normalizeUrl("hello world") == null && HomeTiles.normalizeUrl("nodot") == null, "Bad URL accepted");

        for (int i = 0; i < 40; i++) HomeTiles.add(tiles, new HomeTiles.Tile(HomeTiles.WEB, "https://site" + i + ".com", ""));
        check(tiles.size() == HomeTiles.MAX_TILES, "Tile limit not enforced");
        check(tiles.get(2).label.equals("https://site0.com"), "Empty label should fall back to URL");
        // Responsive grid: every tile row must fit the window width, for any aspect ratio and density.
        int[][] windows = {{1832, 1920, 2}, {1920, 1080, 1}, {3840, 2160, 2}, {1280, 720, 1}, {720, 1280, 1},
                {1080, 2400, 3}, {2400, 1080, 3}, {600, 600, 1}, {3000, 900, 2}, {480, 1600, 2}, {400, 300, 1}};
        for (int[] win : windows) {
            float density = win[2];
            int[] grid = HomeTiles.layoutFor(win[0], win[1], density);
            int columns = grid[0], tile = grid[1];
            float rowDp = columns * (tile / density + 16) + 96;
            check(columns >= 2 && columns <= 8, "Columns out of range for " + win[0] + "x" + win[1]);
            check(rowDp <= win[0] / density + 1, "Row overflows " + win[0] + "x" + win[1] + ": " + columns + " x " + tile);
            check(tile / density >= 48, "Tiles too small for " + win[0] + "x" + win[1]);
            System.out.println("  " + win[0] + "x" + win[1] + "@" + win[2] + "x -> " + columns + " columns, " + Math.round(tile / density) + "dp tiles");
        }
        int[] landscape = HomeTiles.layoutFor(1920, 1080, 1), portrait = HomeTiles.layoutFor(1080, 1920, 1);
        check(landscape[0] > portrait[0], "Landscape panels should show more columns than portrait");
        check(HomeTiles.layoutFor(3840, 2160, 1)[1] > HomeTiles.layoutFor(1280, 720, 1)[1], "Tiles must grow with the window");

        System.out.println("Home tiles persistence, validation, ordering, limits and responsive grid passed");
    }
}
