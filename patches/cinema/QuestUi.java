package com.liskovsoft.smartyoutubetv2.tv.ui.playback.cinema;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
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
    static final int ACCENT = 0xff1a80ff, GLASS = 0xe6121214, BORDER = 0x29ffffff, TRACK = 0x33ffffff;
    static final int MUTED = 0x99ffffff, FILL = 0x12ffffff, FILL_HOVER = 0x2bffffff;

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
        p.setBackground(box(c, GLASS, 24, 1.5f, BORDER));
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
    static Drawable stateBg(Context c, int normal, int focused, float radiusDp, boolean border) {
        StateListDrawable s = new StateListDrawable();
        float stroke = border ? 1 : 0;
        s.addState(new int[]{android.R.attr.state_pressed}, box(c, ACCENT, radiusDp, 0, 0));
        s.addState(new int[]{android.R.attr.state_selected}, box(c, ACCENT, radiusDp, 0, 0));
        s.addState(new int[]{android.R.attr.state_focused}, box(c, focused, radiusDp, 2, ACCENT));
        s.addState(new int[]{android.R.attr.state_hovered}, box(c, focused, radiusDp, stroke, BORDER));
        s.addState(new int[]{}, box(c, normal, radiusDp, stroke, BORDER));
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
        private final String kind;
        private final Paint stroke = new Paint(Paint.ANTI_ALIAS_FLAG), fill = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Path path = new Path();

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
