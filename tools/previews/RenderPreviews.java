import com.liskovsoft.smartyoutubetv2.tv.ui.playback.cinema.CinemaGeometry;
import java.awt.*;
import java.awt.geom.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.Random;
import javax.imageio.ImageIO;

/**
 * Headless Java2D render of the real CinemaGeometry meshes and sky colors.
 * Mirrors CinemaSceneView.onDraw (gradient, sun/moon, stars, faces, halos, 75% brightness),
 * with the virtual screen at the in-app 50% size.
 * These are renders, not screenshots of the app running on a Quest.
 * Usage: java -cp <classes> RenderPreviews <outputDir> [width height]
 */
public class RenderPreviews {
    public static void main(String[] args) throws Exception {
        File out = new File(args.length > 0 ? args[0] : "docs/escenarios");
        int w = args.length > 2 ? Integer.parseInt(args[1]) : 960, h = args.length > 2 ? Integer.parseInt(args[2]) : 540;
        out.mkdirs();
        String[] names = CinemaGeometry.names(true);
        for (int scene = 0; scene < CinemaGeometry.COUNT; scene++) {
            BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
            Graphics2D g = img.createGraphics();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int top = CinemaGeometry.skyTop(scene), horizon = CinemaGeometry.skyHorizon(scene);
            g.setPaint(new GradientPaint(0, 0, new Color(top), 0, h * .55f, new Color(horizon)));
            g.fillRect(0, 0, w, h);
            if (scene == CinemaGeometry.RETRO) {
                circle(g, 0xffffb26b, w * .5f, h * .3f, h * .16f);
                g.setColor(new Color(horizon));
                for (int i = 0; i < 6; i++) g.fill(new Rectangle2D.Float(w * .35f, h * (.3f + i * .022f), w * .3f, h * .006f));
            }
            if (scene == CinemaGeometry.BEACH_SUNSET) circle(g, 0xffffd27a, w * CinemaGeometry.celestialX(scene), h * .37f, h * .1f);
            if (scene == CinemaGeometry.BEACH_TROPICAL) circle(g, 0xfffff4c2, w * CinemaGeometry.celestialX(scene), h * .12f, h * .07f);
            if (scene == CinemaGeometry.BEACH_NIGHT) circle(g, 0xffe9eeff, w * CinemaGeometry.celestialX(scene), h * .14f, h * .06f);
            if (scene == CinemaGeometry.SPACE || scene == CinemaGeometry.CYBER || scene == CinemaGeometry.BEACH_NIGHT) {
                Random rng = new Random(91);
                for (int i = 0; i < 90; i++) circle(g, 0xffc9e4ff, rng.nextFloat() * w, rng.nextFloat() * h * .5f, i % 4 == 0 ? 2 : 1);
            }
            if (scene == CinemaGeometry.MAGIC_FOREST) circle(g, 0xffe9e4ff, w * .78f, h * .14f, h * .06f);
            for (CinemaGeometry.Face f : CinemaGeometry.build(scene, false)) {
                Path2D p = new Path2D.Float();
                for (int i = 0; i < 9; i += 3) {
                    float[] q = CinemaGeometry.project(f.vertices[i], f.vertices[i + 1], f.vertices[i + 2], w, h);
                    if (i == 0) p.moveTo(q[0], q[1]); else p.lineTo(q[0], q[1]);
                }
                p.closePath();
                g.setColor(new Color(f.color, true));
                g.fill(p);
            }
            if (scene == CinemaGeometry.MAGIC_FOREST) {
                Random rng = new Random(23);
                for (int i = 0; i < 36; i++) {
                    float x = rng.nextFloat() * w, y = h * (.25f + rng.nextFloat() * .6f), r = h * (.006f + rng.nextFloat() * .01f);
                    circle(g, i % 2 == 0 ? 0x55d9ff6a : 0x55fff3a0, x, y, r * 2.6f);
                    circle(g, i % 2 == 0 ? 0xffd9ff6a : 0xfffff3a0, x, y, r);
                }
            }
            if (scene == CinemaGeometry.LOUNGE_NIGHT) {
                circle(g, 0x22ffb35a, w * .12f, h * .55f, h * .28f);
                circle(g, 0x22ffb35a, w * .88f, h * .55f, h * .28f);
            }
            g.setColor(new Color(0, 0, 0, 25 * 255 / 100));
            g.fillRect(0, 0, w, h);
            // Virtual screen at the in-app "50 %" size (3 m), where SmartTube's video plays.
            int vw = Math.round(w * .5f), vh = Math.round(h * .5f);
            g.setColor(new Color(0x0b0f17));
            g.fillRoundRect((w - vw) / 2, (h - vh) / 2, vw, vh, 8, 8);
            g.setColor(new Color(0x8a96a8));
            g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, Math.max(14, h / 22)));
            FontMetrics fm = g.getFontMetrics();
            String label = names[scene] + " · render";
            g.drawString(label, (w - fm.stringWidth(label)) / 2, h / 2 + fm.getAscent() / 2);
            g.dispose();
            ImageIO.write(img, "png", new File(out, String.format("%02d-%s.png", scene, slug(CinemaGeometry.names(false)[scene]))));
        }
        System.out.println("Rendered " + CinemaGeometry.COUNT + " sceneries into " + out);
    }
    private static void circle(Graphics2D g, int argb, float x, float y, float r) {
        g.setColor(new Color(argb, true));
        g.fill(new Ellipse2D.Float(x - r, y - r, r * 2, r * 2));
    }
    static String slug(String s) { return s.toLowerCase().replaceAll("[^a-z0-9]+", "-").replaceAll("(^-|-$)", ""); }
}
