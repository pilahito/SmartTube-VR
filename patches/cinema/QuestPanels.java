package com.liskovsoft.smartyoutubetv2.tv.ui.playback.cinema;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.TextView;
import java.io.InputStream;

/**
 * Panels of the SmartTube VR player, styled after docs/interfaz/reproductor.jpg, escenarios.jpg and
 * pantalla-ajustes.jpg: "Reproductor VR" (options), "Escenarios" (4x4 grid with the real scenery renders)
 * and "Pantalla virtual". They only build views and report choices; QuestCinemaController owns the state.
 */
final class QuestPanels {
    private QuestPanels() {}

    // ------------------------------------------------------------------ Reproductor VR (opciones)

    interface OptionsActions {
        void videoSettings(); void subtitles();
        void scenes(); void screen(); void brightness(); void toggleEco(); void sponsor(); void dearrow();
        void saveFavorite(); void openFavorite(); void toggleScenery(); void toggleClassic(); void more(); void close();
    }

    /**
     * "Reproductor VR" as in the mockup: three rows (video settings, subtitles, more options). With {@code more}
     * the same panel lists the rest (virtual screen, scenery, brightness, ECO, SponsorBlock, DeArrow, favorites...).
     * {@code subtitleValue} is the current subtitle language; null hides the row (no SmartTube player behind).
     */
    static View options(Context c, boolean es, boolean more, String subtitle, String subtitleValue, int brightness, boolean eco, boolean sceneryOn, boolean classic, OptionsActions a) {
        LinearLayout panel = QuestUi.glassPanel(c);
        LinearLayout head = new LinearLayout(c);
        head.setGravity(Gravity.CENTER_VERTICAL);
        head.addView(new QuestUi.Icon(c, "monitor", Color.WHITE), new LinearLayout.LayoutParams(QuestUi.dp(c, 44), QuestUi.dp(c, 44)));
        LinearLayout titles = new LinearLayout(c);
        titles.setOrientation(LinearLayout.VERTICAL);
        titles.setPadding(QuestUi.dp(c, 12), 0, 0, 0);
        titles.addView(QuestUi.label(c, more ? (es ? "Más opciones" : "More options") : (es ? "Reproductor VR" : "VR Player"), 22, Color.WHITE));
        titles.addView(QuestUi.label(c, subtitle, 13, QuestUi.MUTED));
        head.addView(titles, new LinearLayout.LayoutParams(0, -2, 1f));
        head.addView(closeButton(c, a::close), new LinearLayout.LayoutParams(QuestUi.dp(c, 44), QuestUi.dp(c, 44)));
        panel.addView(head);
        panel.addView(spacer(c, 10));
        panel.addView(QuestUi.divider(c));
        panel.addView(spacer(c, 12));
        if (!more) panel.addView(QuestUi.label(c, es ? "Opciones" : "Options", 15, Color.WHITE));
        panel.addView(spacer(c, 4));
        String off = es ? "desactivado" : "off", on = es ? "activado" : "on";
        if (!more) {
            row(c, panel, "visor", es ? "Ajustes de video" : "Video settings", null, a::videoSettings, true);
            if (subtitleValue != null) row(c, panel, "subtitles", es ? "Subtítulos" : "Subtitles", subtitleValue, a::subtitles, true);
            row(c, panel, "gear", es ? "Más opciones" : "More options", null, a::more, false);
        } else {
            row(c, panel, "screen_curved", es ? "Pantalla virtual" : "Virtual screen", null, a::screen, true);
            row(c, panel, "scenes", es ? "Escenarios" : "Scenery", null, a::scenes, true);
            row(c, panel, "sun", es ? "Brillo del escenario" : "Scenery brightness", brightness + "%", a::brightness, true);
            row(c, panel, "bolt", "ECO", eco ? on : off, a::toggleEco, true);
            row(c, panel, "skip", "SponsorBlock / AdStop", null, a::sponsor, true);
            row(c, panel, "frame", "DeArrow", null, a::dearrow, true);
            row(c, panel, "star", es ? "Guardar escenario favorito" : "Save favorite scenery", null, a::saveFavorite, true);
            row(c, panel, "star", es ? "Abrir favorito" : "Open favorite", null, a::openFavorite, true);
            row(c, panel, "gear", es ? "Controles de SmartTube" : "SmartTube controls", classic ? (es ? "clásicos" : "classic") : "VR", a::toggleClassic, true);
            row(c, panel, "monitor", sceneryOn ? (es ? "Pantalla completa sin escenario" : "Fullscreen without scenery")
                    : (es ? "Activar escenario" : "Enable scenery"), null, a::toggleScenery, false);
        }
        ScrollView scroll = new ScrollView(c);
        scroll.setFillViewport(true);
        scroll.addView(panel);
        return scroll;
    }

    private static void row(Context c, LinearLayout list, String icon, String text, String value, Runnable action, boolean divider) {
        LinearLayout r = new LinearLayout(c);
        r.setGravity(Gravity.CENTER_VERTICAL);
        r.setPadding(QuestUi.dp(c, 6), 0, QuestUi.dp(c, 10), 0);
        r.setMinimumHeight(QuestUi.dp(c, 52));
        r.setBackground(QuestUi.stateBg(c, Color.TRANSPARENT, QuestUi.FILL_HOVER, 14, false));
        r.setFocusable(true);
        r.setClickable(true);
        r.setOnClickListener(v -> action.run());
        r.addView(new QuestUi.Icon(c, icon, Color.WHITE), new LinearLayout.LayoutParams(QuestUi.dp(c, 34), QuestUi.dp(c, 34)));
        TextView t = QuestUi.label(c, text, 16, Color.WHITE);
        t.setPadding(QuestUi.dp(c, 12), 0, 0, 0);
        r.addView(t, new LinearLayout.LayoutParams(0, -2, 1f));
        if (value != null) r.addView(QuestUi.label(c, value, 15, QuestUi.MUTED));
        r.addView(new QuestUi.Icon(c, "chevron", QuestUi.MUTED), new LinearLayout.LayoutParams(QuestUi.dp(c, 30), QuestUi.dp(c, 30)));
        list.addView(r, new LinearLayout.LayoutParams(-1, -2));
        if (divider) list.addView(QuestUi.divider(c));
    }

    private static View closeButton(Context c, Runnable onClick) {
        QuestUi.Icon x = new QuestUi.Icon(c, "close", Color.WHITE);
        x.setBackground(QuestUi.stateBg(c, Color.TRANSPARENT, QuestUi.FILL_HOVER, 14, false));
        x.setFocusable(true);
        x.setClickable(true);
        x.setContentDescription("Cerrar");
        x.setOnClickListener(v -> onClick.run());
        return x;
    }

    private static View spacer(Context c, int dp) {
        View v = new View(c);
        v.setLayoutParams(new LinearLayout.LayoutParams(1, QuestUi.dp(c, dp)));
        return v;
    }

    // ------------------------------------------------------------------ Escenarios (cuadrícula 4x4)

    interface ScenesListener { void choose(int scene); void close(); }

    static final class ScenesPanel {
        final View view;
        private final View[] tiles = new View[CinemaGeometry.COUNT];

        ScenesPanel(Context c, boolean es, int current, ScenesListener l) {
            LinearLayout panel = QuestUi.glassPanel(c);
            LinearLayout head = new LinearLayout(c);
            head.setGravity(Gravity.CENTER_VERTICAL);
            head.addView(QuestUi.label(c, es ? "Escenarios" : "Scenery", 26, Color.WHITE), new LinearLayout.LayoutParams(0, -2, 1f));
            head.addView(closeButton(c, l::close), new LinearLayout.LayoutParams(QuestUi.dp(c, 44), QuestUi.dp(c, 44)));
            panel.addView(head);
            panel.addView(spacer(c, 8));
            panel.addView(QuestUi.divider(c));
            panel.addView(spacer(c, 10));
            String[] names = CinemaGeometry.names(es);
            for (int row = 0; row < 4; row++) {
                LinearLayout line = new LinearLayout(c);
                for (int col = 0; col < 4; col++) {
                    final int scene = row * 4 + col;
                    if (scene >= CinemaGeometry.COUNT) break;
                    View tile = tile(c, scene, names[scene], l);
                    tiles[scene] = tile;
                    LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(0, QuestUi.dp(c, 112), 1f);
                    p.setMargins(QuestUi.dp(c, 4), QuestUi.dp(c, 4), QuestUi.dp(c, 4), QuestUi.dp(c, 4));
                    line.addView(tile, p);
                }
                panel.addView(line, new LinearLayout.LayoutParams(-1, -2));
            }
            ScrollView scroll = new ScrollView(c);
            scroll.setFillViewport(true);
            scroll.addView(panel);
            view = scroll;
            select(current);
        }

        void select(int scene) {
            for (int i = 0; i < tiles.length; i++) if (tiles[i] != null) tiles[i].setSelected(i == scene);
        }

        private static View tile(Context c, int scene, String name, ScenesListener l) {
            FrameLayout f = new FrameLayout(c);
            f.setFocusable(true);
            f.setClickable(true);
            f.setOnClickListener(v -> l.choose(scene));
            ImageView img = new ImageView(c);
            img.setScaleType(ImageView.ScaleType.CENTER_CROP);
            Bitmap bmp = load(c, scene);
            if (bmp != null) img.setImageBitmap(bmp);
            else img.setImageDrawable(new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,
                    new int[]{CinemaGeometry.skyTop(scene), CinemaGeometry.skyHorizon(scene)}));
            f.addView(img, new FrameLayout.LayoutParams(-1, -1));
            TextView cap = QuestUi.label(c, name, 14, Color.WHITE);
            cap.setGravity(Gravity.CENTER);
            cap.setSingleLine(true);
            cap.setPadding(QuestUi.dp(c, 4), QuestUi.dp(c, 5), QuestUi.dp(c, 4), QuestUi.dp(c, 5));
            cap.setBackgroundColor(0x99000000);
            f.addView(cap, new FrameLayout.LayoutParams(-1, -2, Gravity.BOTTOM));
            // Border on top of the image: thin by default, thick blue when selected / focused / hovered.
            f.setForeground(borderStates(c));
            return f;
        }

        private static android.graphics.drawable.Drawable borderStates(Context c) {
            android.graphics.drawable.StateListDrawable s = new android.graphics.drawable.StateListDrawable();
            s.addState(new int[]{android.R.attr.state_selected}, QuestUi.box(c, Color.TRANSPARENT, 8, 3, QuestUi.accent));
            s.addState(new int[]{android.R.attr.state_focused}, QuestUi.box(c, Color.TRANSPARENT, 8, 3, QuestUi.accent));
            s.addState(new int[]{android.R.attr.state_hovered}, QuestUi.box(c, Color.TRANSPARENT, 8, 3, QuestUi.accent));
            s.addState(new int[]{}, QuestUi.box(c, Color.TRANSPARENT, 8, 1, QuestUi.border));
            return s;
        }

        /** Thumbnail from assets/vr_scenes/scene_NN.jpg (added by the cinema patch); null if missing. */
        private static Bitmap load(Context c, int scene) {
            try (InputStream in = c.getAssets().open(String.format(java.util.Locale.US, "vr_scenes/scene_%02d.jpg", scene))) {
                return BitmapFactory.decodeStream(in);
            } catch (Exception e) {
                return null;
            }
        }
    }

    // ------------------------------------------------------------------ Pantalla virtual

    interface ScreenListener {
        void curved(boolean on); void curvature(int percent); void format(int mode);
        void size(int percent); void distance(int meters); void height(int value); void preset(int which);
    }

    /** Format chips of the panel, in the mockup's order ("Rellenar" stays in SmartTube's own aspect menu). */
    static final int[] PANEL_FORMATS = {VirtualScreen.R21_9, VirtualScreen.R16_9, VirtualScreen.R32_9, VirtualScreen.R4_3, VirtualScreen.FIT};

    static final class ScreenPanel {
        final View view;
        private final View curvedBtn, flatBtn;
        private final TextView[] formatChips = new TextView[VirtualScreen.MODE_COUNT];
        private final SeekBar curvature, size, distance, height;
        private final TextView sizeValue, distanceValue;

        ScreenPanel(Context c, boolean es, boolean curved, int curv, int format, int sizePct, int dist, int heightValue, ScreenListener l) {
            LinearLayout panel = QuestUi.glassPanel(c);
            panel.setPadding(QuestUi.dp(c, 26), QuestUi.dp(c, 22), QuestUi.dp(c, 26), QuestUi.dp(c, 22));
            panel.addView(QuestUi.label(c, es ? "Pantalla virtual" : "Virtual screen", 30, Color.WHITE));
            panel.addView(spacer(c, 14));

            LinearLayout shape = new LinearLayout(c);
            curvedBtn = shapeButton(c, es ? "Curva" : "Curved", "screen_curved", () -> { l.curved(true); setCurved(true); });
            flatBtn = shapeButton(c, es ? "Plana" : "Flat", "screen_flat", () -> { l.curved(false); setCurved(false); });
            shape.addView(curvedBtn, weight(c, 1f, 4));
            shape.addView(flatBtn, weight(c, 1f, 4));
            panel.addView(shape, new LinearLayout.LayoutParams(-1, QuestUi.dp(c, 60)));
            panel.addView(spacer(c, 12));

            // Curvature has no number in the mockup: the slider alone.
            curvature = sliderRow(c, panel, es ? "Curvatura" : "Curvature", null,
                    VirtualScreen.CURVATURE_MAX - VirtualScreen.CURVATURE_MIN, VirtualScreen.clampCurvature(curv) - VirtualScreen.CURVATURE_MIN,
                    v -> l.curvature(v + VirtualScreen.CURVATURE_MIN));

            panel.addView(QuestUi.label(c, es ? "Formato" : "Format", 18, Color.WHITE));
            panel.addView(spacer(c, 4));
            String[] names = VirtualScreen.modeNames(es);
            LinearLayout chips = new LinearLayout(c);
            for (int mode : PANEL_FORMATS) {
                final int m = mode;
                boolean wide = mode == VirtualScreen.R21_9;
                String label = wide ? names[mode] + "  " + (es ? "Ultrapanorámica" : "Ultrawide") : names[mode];
                formatChips[mode] = QuestUi.chip(c, label, mode == format, () -> { l.format(m); setFormat(m); });
                chips.addView(formatChips[mode], weight(c, wide ? 2.4f : 1f, 3));
            }
            panel.addView(chips, new LinearLayout.LayoutParams(-1, QuestUi.dp(c, 46)));
            panel.addView(spacer(c, 12));

            sizeValue = QuestUi.label(c, "", 16, QuestUi.MUTED);
            size = sliderRow(c, panel, es ? "Tamaño" : "Size", sizeValue, 10, (VirtualScreen.clampSize(sizePct) - 50) / 5, v -> {
                int pct = 50 + v * 5; sizeValue.setText(pct + " %"); l.size(pct); });
            sizeValue.setText(VirtualScreen.clampSize(sizePct) + " %");
            distanceValue = QuestUi.label(c, "", 16, QuestUi.MUTED);
            distance = sliderRow(c, panel, es ? "Distancia" : "Distance", distanceValue, 3, VirtualScreen.clampDistance(dist) - 2, v -> {
                int m = 2 + v; distanceValue.setText(m + " m"); l.distance(m); });
            distanceValue.setText(VirtualScreen.clampDistance(dist) + " m");
            height = sliderRow(c, panel, es ? "Altura" : "Height", null,
                    VirtualScreen.HEIGHT_MAX - VirtualScreen.HEIGHT_MIN, VirtualScreen.clampHeight(heightValue) - VirtualScreen.HEIGHT_MIN,
                    v -> l.height(v + VirtualScreen.HEIGHT_MIN));

            panel.addView(QuestUi.label(c, es ? "Preajustes" : "Presets", 18, Color.WHITE));
            panel.addView(spacer(c, 4));
            LinearLayout presets = new LinearLayout(c);
            String[] pn = VirtualScreen.presetNames(es);
            for (int i = 0; i < VirtualScreen.PRESET_COUNT; i++) {
                final int which = i;
                presets.addView(QuestUi.chip(c, pn[i], false, () -> { l.preset(which); applyPreset(which); }), weight(c, 1f, 3));
            }
            panel.addView(presets, new LinearLayout.LayoutParams(-1, QuestUi.dp(c, 46)));
            ScrollView scroll = new ScrollView(c);
            scroll.setFillViewport(true);
            scroll.addView(panel);
            view = scroll;
            setCurved(curved);
            setFormat(format);
        }

        private static LinearLayout.LayoutParams weight(Context c, float w, int marginDp) {
            LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(0, -1, w);
            p.setMargins(QuestUi.dp(c, marginDp), QuestUi.dp(c, 2), QuestUi.dp(c, marginDp), QuestUi.dp(c, 2));
            return p;
        }

        /** Big "Curva" / "Plana" button: icon + text; selecting the row also selects (tints) its children. */
        private static View shapeButton(Context c, String text, String icon, Runnable onClick) {
            LinearLayout b = new LinearLayout(c);
            b.setGravity(Gravity.CENTER);
            b.setBackground(QuestUi.stateBg(c, QuestUi.FILL, QuestUi.FILL_HOVER, 14, true));
            b.setFocusable(true);
            b.setClickable(true);
            b.setContentDescription(text);
            b.setOnClickListener(v -> onClick.run());
            b.addView(new QuestUi.Icon(c, icon, Color.WHITE), new LinearLayout.LayoutParams(QuestUi.dp(c, 40), QuestUi.dp(c, 40)));
            TextView t = QuestUi.label(c, text, 18, Color.WHITE);
            t.setPadding(QuestUi.dp(c, 10), 0, 0, 0);
            b.addView(t);
            return b;
        }

        private static SeekBar sliderRow(Context c, LinearLayout parent, String name, TextView value, int max, int progress, QuestUi.IntListener l) {
            LinearLayout line = new LinearLayout(c);
            line.addView(QuestUi.label(c, name, 18, Color.WHITE), new LinearLayout.LayoutParams(0, -2, 1f));
            if (value != null) line.addView(value);
            parent.addView(line, new LinearLayout.LayoutParams(-1, -2));
            SeekBar bar = QuestUi.slider(c, max, progress, QuestUi.accent, 5, 20, l);
            parent.addView(bar, new LinearLayout.LayoutParams(-1, QuestUi.dp(c, 32)));
            return bar;
        }

        private void setCurved(boolean on) { curvedBtn.setSelected(on); flatBtn.setSelected(!on); curvature.setEnabled(on); }

        private void setFormat(int mode) { for (int i = 0; i < formatChips.length; i++) if (formatChips[i] != null) formatChips[i].setSelected(i == mode); }

        /** Mirrors a preset in the controls (the controller applies the values). */
        private void applyPreset(int which) {
            int[] p = VirtualScreen.preset(which);
            size.setProgress((p[0] - 50) / 5); sizeValue.setText(p[0] + " %");
            distance.setProgress(p[1] - 2); distanceValue.setText(p[1] + " m");
            curvature.setProgress(p[3] - VirtualScreen.CURVATURE_MIN);
            height.setProgress(p[5] - VirtualScreen.HEIGHT_MIN);
            setCurved(p[2] == 1); setFormat(p[4]);
        }
    }
}
