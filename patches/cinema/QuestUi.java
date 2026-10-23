package com.liskovsoft.smartyoutubetv2.tv.ui.playback.cinema;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.drawable.ClipDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.LayerDrawable;
import android.graphics.drawable.StateListDrawable;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;

/**
 * Shared look of the SmartTube VR panels (docs/interfaz/*.jpg): dark glass, thin borders, blue accent.
 * Only android.* is used so the classes compile on their own against android.jar.
 */
final class QuestUi {
    static final int GLASS = 0xe6121214, TRACK = 0x33ffffff;
    static final int MUTED = 0x99ffffff, FILL = 0x12ffffff, FILL_HOVER = 0x2bffffff;
    // Theme colors: change with the scenery (see QuestTheme). Panels read them when they are built.
    static int accent = 0xff1a80ff, border = 0x29ffffff, progress = 0xffe03333;

    interface IntListener { void on(int value); }

    private QuestUi() {}

    static int dp(Context c, float n) { return Math.round(n * c.getResources().getDisplayMetrics().density); }

    static GradientDrawable box(Context c, int color, float radiusDp, float strokeDp, int strokeColor) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(color);
        d.setCornerRadius(dp(c, radiusDp));
        if (strokeDp > 0) d.setStroke(Math.max(1, dp(c, strokeDp)), strokeColor);
        return d;
    }

    /** Vertical glass panel with the padding used by every panel. */
    static LinearLayout glassPanel(Context c) {
        LinearLayout p = new LinearLayout(c);
        p.setOrientation(LinearLayout.VERTICAL);
        p.setPadding(dp(c, 22), dp(c, 18), dp(c, 22), dp(c, 18));
        p.setBackground(box(c, GLASS, 24, 1.5f, border));
        return p;
    }

    static TextView label(Context c, String text, float sp, int color) {
        TextView t = new TextView(c);
        t.setText(text);
        t.setTextSize(sp);
        t.setTextColor(color);
        return t;
    }

    static View divider(Context c) {
        View v = new View(c);
        v.setBackgroundColor(0x1affffff);
        v.setLayoutParams(new LinearLayout.LayoutParams(-1, Math.max(1, dp(c, 1))));
        return v;
    }

    /** Pressed / focused / normal background for clickable rows, chips and buttons. */
    static Drawable stateBg(Context c, int normal, int focused, float radiusDp, boolean outline) {
        StateListDrawable s = new StateListDrawable();
        float stroke = outline ? 1 : 0;
        s.addState(new int[]{android.R.attr.state_pressed}, box(c, accent, radiusDp, 0, 0));
        s.addState(new int[]{android.R.attr.state_selected}, box(c, accent, radiusDp, 0, 0));
        s.addState(new int[]{android.R.attr.state_focused}, box(c, focused, radiusDp, 2, accent));
        s.addState(new int[]{android.R.attr.state_hovered}, box(c, focused, radiusDp, stroke, border));
        s.addState(new int[]{}, box(c, normal, radiusDp, stroke, border));
        return s;
    }

    /** Selectable chip (format, preset...). The selected one is filled blue. */
    static TextView chip(Context c, String text, boolean selected, Runnable onClick) {
        TextView t = label(c, text, 14, Color.WHITE);
        t.setGravity(Gravity.CENTER);
        t.setSingleLine(true);
        t.setPadding(dp(c, 10), dp(c, 9), dp(c, 10), dp(c, 9));
        t.setBackground(stateBg(c, FILL, FILL_HOVER, 12, true));
        t.setSelected(selected);
        t.setFocusable(true);
        t.setClickable(true);
        t.setOnClickListener(v -> onClick.run());
        return t;
    }

    /** Thin slider with a round thumb; thumbDp = 0 hides the thumb (progress bar look). */
    static SeekBar slider(Context c, int max, int progress, int fill, float trackDp, float thumbDp, IntListener l) {
        SeekBar b = new SeekBar(c);
        b.setMax(max);
        b.setProgress(progress);
        GradientDrawable track = box(c, TRACK, trackDp, 0, 0);
        track.setSize(-1, dp(c, trackDp));
        GradientDrawable on = box(c, fill, trackDp, 0, 0);
        on.setSize(-1, dp(c, trackDp));
        LayerDrawable layers = new LayerDrawable(new Drawable[]{track, new ClipDrawable(on, Gravity.START, ClipDrawable.HORIZONTAL)});
        layers.setId(0, android.R.id.background);
        layers.setId(1, android.R.id.progress);
        b.setProgressDrawable(layers);
        GradientDrawable thumb = new GradientDrawable();
        thumb.setShape(GradientDrawable.OVAL);
        thumb.setColor(thumbDp > 0 ? fill : Color.TRANSPARENT);
        int ts = Math.max(1, dp(c, thumbDp));
        thumb.setSize(ts, ts);
        b.setThumb(thumb);
        b.setPadding(ts / 2, dp(c, 6), ts / 2, dp(c, 6));
        b.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar s, int p, boolean fromUser) { if (fromUser) l.on(p); }
            @Override public void onStartTrackingTouch(SeekBar s) {}
            @Override public void onStopTrackingTouch(SeekBar s) {}
        });
        return b;
    }

    /** Icons drawn with Canvas (no image assets), same shapes as the Godot panels. */
    static final class Icon extends View {
        private String kind;
        private final Paint stroke = new Paint(Paint.ANTI_ALIAS_FLAG), fill = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Path path = new Path();
        private int level = 100;

        void setKind(String kind) { if (!kind.equals(this.kind)) { this.kind = kind; invalidate(); } }

        /** Battery fill (0-100) or Wi-Fi bars (0-100 mapped to 0-4). */
        void setLevel(int level) { this.level = Math.max(0, Math.min(100, level)); invalidate(); }

        Icon(Context c, String kind, int color) {
            super(c);
            this.kind = kind;
            stroke.setStyle(Paint.Style.STROKE);
            stroke.setStrokeCap(Paint.Cap.ROUND);
            stroke.setStrokeJoin(Paint.Join.ROUND);
            stroke.setColor(color);
            fill.setStyle(Paint.Style.FILL);
            fill.setColor(color);
        }

        @Override protected void onDraw(Canvas g) {
            float w = getWidth(), h = getHeight(), cx = w / 2f, cy = h / 2f, s = Math.min(w, h) * .3f;
            stroke.setStrokeWidth(Math.max(2f, Math.min(w, h) * .085f));
            switch (kind) {
                case "close":
                    g.drawLine(cx - s, cy - s, cx + s, cy + s, stroke);
                    g.drawLine(cx - s, cy + s, cx + s, cy - s, stroke);
                    break;
                case "chevron":
                    path.reset(); path.moveTo(cx - s * .5f, cy - s); path.lineTo(cx + s * .5f, cy); path.lineTo(cx - s * .5f, cy + s);
                    g.drawPath(path, stroke);
                    break;
                case "monitor":
                    g.drawRect(cx - s * 1.1f, cy - s * .9f, cx + s * 1.1f, cy + s * .6f, stroke);
                    g.drawLine(cx, cy + s * .6f, cx, cy + s, stroke);
                    g.drawLine(cx - s * .6f, cy + s, cx + s * .6f, cy + s, stroke);
                    break;
                case "screen_curved":
                    path.reset(); curve(cx, cy - s * .8f, s); g.drawPath(path, stroke);
                    path.reset(); curve(cx, cy + s * .35f, s); g.drawPath(path, stroke);
                    g.drawLine(cx - s * 1.15f, cy - s * .8f, cx - s * 1.15f, cy + s * .35f, stroke);
                    g.drawLine(cx + s * 1.15f, cy - s * .8f, cx + s * 1.15f, cy + s * .35f, stroke);
                    break;
                case "screen_flat":
                    g.drawRect(cx - s * 1.1f, cy - s * .8f, cx + s * 1.1f, cy + s * .8f, stroke);
                    break;
                case "scenes":
                    for (int i = 0; i < 2; i++) for (int j = 0; j < 2; j++)
                        g.drawRect(cx - s * 1.05f + i * s * 1.1f, cy - s * 1.05f + j * s * 1.1f, cx - s * .15f + i * s * 1.1f, cy - s * .15f + j * s * 1.1f, stroke);
                    break;
                case "sun":
                    g.drawCircle(cx, cy, s * .55f, stroke);
                    for (int i = 0; i < 8; i++) {
                        double a = Math.PI * 2 * i / 8;
                        g.drawLine(cx + (float) Math.cos(a) * s * .85f, cy + (float) Math.sin(a) * s * .85f,
                                cx + (float) Math.cos(a) * s * 1.2f, cy + (float) Math.sin(a) * s * 1.2f, stroke);
                    }
                    break;
                case "bolt":
                    path.reset(); path.moveTo(cx + s * .3f, cy - s * 1.1f); path.lineTo(cx - s * .6f, cy + s * .15f);
                    path.lineTo(cx, cy + s * .15f); path.lineTo(cx - s * .3f, cy + s * 1.1f); path.lineTo(cx + s * .6f, cy - s * .15f);
                    path.lineTo(cx, cy - s * .15f); path.close();
                    g.drawPath(path, fill);
                    break;
                case "skip":
                    for (int i = 0; i < 2; i++) {
                        float x = cx - s * .9f + i * s * .8f;
                        path.reset(); path.moveTo(x, cy - s * .7f); path.lineTo(x + s * .8f, cy); path.lineTo(x, cy + s * .7f); path.close();
                        g.drawPath(path, fill);
                    }
                    g.drawLine(cx + s * .95f, cy - s * .7f, cx + s * .95f, cy + s * .7f, stroke);
                    break;
                case "star":
                    path.reset();
                    for (int i = 0; i < 10; i++) {
                        double a = -Math.PI / 2 + Math.PI * i / 5;
                        float r = i % 2 == 0 ? s * 1.15f : s * .5f;
                        float x = cx + (float) Math.cos(a) * r, y = cy + (float) Math.sin(a) * r;
                        if (i == 0) path.moveTo(x, y); else path.lineTo(x, y);
                    }
                    path.close();
                    g.drawPath(path, stroke);
                    break;
                case "frame":
                    g.drawRect(cx - s * 1.1f, cy - s * .8f, cx + s * 1.1f, cy + s * .8f, stroke);
                    g.drawLine(cx - s * 1.1f, cy + s * .45f, cx - s * .3f, cy - s * .1f, stroke);
                    g.drawLine(cx - s * .3f, cy - s * .1f, cx + s * .2f, cy + s * .3f, stroke);
                    g.drawLine(cx + s * .2f, cy + s * .3f, cx + s * .6f, cy, stroke);
                    break;
                case "menu":
                    for (int i = -1; i <= 1; i++) g.drawLine(cx - s, cy + i * s * .7f, cx + s, cy + i * s * .7f, stroke);
                    break;
                case "play":
                    path.reset(); path.moveTo(cx - s * .7f, cy - s); path.lineTo(cx - s * .7f, cy + s); path.lineTo(cx + s, cy); path.close();
                    g.drawPath(path, fill);
                    break;
                case "pause":
                    g.drawRect(cx - s * .8f, cy - s, cx - s * .25f, cy + s, fill);
                    g.drawRect(cx + s * .25f, cy - s, cx + s * .8f, cy + s, fill);
                    break;
                case "next":
                    path.reset(); path.moveTo(cx - s * .9f, cy - s); path.lineTo(cx - s * .9f, cy + s); path.lineTo(cx + s * .5f, cy); path.close();
                    g.drawPath(path, fill);
                    g.drawRect(cx + s * .65f, cy - s, cx + s * .95f, cy + s, fill);
                    break;
                case "thumb_up": case "thumb_down": {
                    float f = kind.equals("thumb_up") ? 1f : -1f;
                    g.drawRect(cx - s, cy - f * s * .1f - (f > 0 ? 0 : s * .8f), cx - s * .45f, cy + f * s * .9f - (f > 0 ? 0 : s * .8f), fill);
                    path.reset();
                    path.moveTo(cx - s * .3f, cy - f * s * .05f); path.lineTo(cx + s * .1f, cy - f * s); path.lineTo(cx + s * .55f, cy - f * s * .8f);
                    path.lineTo(cx + s * .35f, cy - f * s * .1f); path.lineTo(cx + s * 1.05f, cy - f * s * .1f); path.lineTo(cx + s * .85f, cy + f * s * .85f);
                    path.lineTo(cx - s * .3f, cy + f * s * .85f); path.close();
                    g.drawPath(path, fill);
                    break;
                }
                case "gear":
                    g.drawCircle(cx, cy, s * .7f, stroke);
                    for (int i = 0; i < 8; i++) {
                        double a = Math.PI * 2 * i / 8;
                        g.drawLine(cx + (float) Math.cos(a) * s * .85f, cy + (float) Math.sin(a) * s * .85f,
                                cx + (float) Math.cos(a) * s * 1.2f, cy + (float) Math.sin(a) * s * 1.2f, stroke);
                    }
                    break;
                case "expand":
                    for (int sx = -1; sx <= 1; sx += 2) for (int sy = -1; sy <= 1; sy += 2) {
                        float x = cx + sx * s, y = cy + sy * s;
                        g.drawLine(x, y, x - sx * s * .6f, y, stroke);
                        g.drawLine(x, y, x, y - sy * s * .6f, stroke);
                    }
                    break;
                case "wifi": {
                    int bars = Math.round(level / 25f);
                    for (int i = 1; i <= 3; i++) {
                        stroke.setAlpha(i <= bars ? 255 : 90);
                        float r = s * (.55f * i + .1f);
                        g.drawArc(cx - r, cy + s * .7f - r, cx + r, cy + s * .7f + r, -135, 90, false, stroke);
                    }
                    stroke.setAlpha(255);
                    g.drawCircle(cx, cy + s * .7f, s * .12f, fill);
                    break;
                }
                case "battery": {
                    g.drawRect(cx - s * 1.2f, cy - s * .6f, cx + s * 1.0f, cy + s * .6f, stroke);
                    g.drawRect(cx + s * 1.0f, cy - s * .25f, cx + s * 1.25f, cy + s * .25f, fill);
                    g.drawRect(cx - s * 1.05f, cy - s * .45f, cx - s * 1.05f + (s * 1.9f) * level / 100f, cy + s * .45f, fill);
                    break;
                }
                case "meta": {
                    // Meta logo: an infinity loop.
                    path.reset();
                    path.moveTo(cx, cy);
                    path.cubicTo(cx - s * .45f, cy - s * .9f, cx - s * 1.25f, cy - s * .7f, cx - s * 1.25f, cy);
                    path.cubicTo(cx - s * 1.25f, cy + s * .7f, cx - s * .45f, cy + s * .9f, cx, cy);
                    path.cubicTo(cx + s * .45f, cy - s * .9f, cx + s * 1.25f, cy - s * .7f, cx + s * 1.25f, cy);
                    path.cubicTo(cx + s * 1.25f, cy + s * .7f, cx + s * .45f, cy + s * .9f, cx, cy);
                    g.drawPath(path, stroke);
                    break;
                }
                case "visor": {
                    // VR headset seen from the front: rounded body, two lenses and the nose cut-out.
                    RectF body = new RectF(cx - s * 1.15f, cy - s * .6f, cx + s * 1.15f, cy + s * .6f);
                    g.drawRoundRect(body, s * .45f, s * .45f, stroke);
                    g.drawCircle(cx - s * .5f, cy, s * .28f, fill);
                    g.drawCircle(cx + s * .5f, cy, s * .28f, fill);
                    g.drawLine(cx - s * .2f, cy + s * .6f, cx, cy + s * .3f, stroke);
                    g.drawLine(cx, cy + s * .3f, cx + s * .2f, cy + s * .6f, stroke);
                    break;
                }
                case "subtitles": {
                    g.drawCircle(cx, cy, s * 1.15f, stroke);
                    g.drawLine(cx - s * .55f, cy - s * .15f, cx + s * .55f, cy - s * .15f, stroke);
                    g.drawLine(cx - s * .55f, cy + s * .3f, cx + s * .2f, cy + s * .3f, stroke);
                    break;
                }
                case "undo": {
                    RectF r = new RectF(cx - s * .9f, cy - s * .8f, cx + s * .9f, cy + s);
                    g.drawArc(r, 200, 250, false, stroke);
                    path.reset();
                    path.moveTo(cx - s * 1.15f, cy - s * .55f);
                    path.lineTo(cx - s * .35f, cy - s * .55f);
                    path.lineTo(cx - s * .85f, cy + s * .1f);
                    path.close();
                    g.drawPath(path, fill);
                    break;
                }
                default:
                    break;
            }
        }

        private void curve(float cx, float y, float s) {
            float w = s * 1.15f;
            path.moveTo(cx - w, y);
            path.quadTo(cx, y + s * .9f, cx + w, y);
        }
    }
}
