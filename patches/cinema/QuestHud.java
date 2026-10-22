package com.liskovsoft.smartyoutubetv2.tv.ui.playback.cinema;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * The player HUD of docs/interfaz/reproductor.jpg drawn over SmartTube's player: status pill (device, Wi-Fi,
 * battery, menu) at the top left and the control bar at the bottom (play, next, time, undo, quality and DeArrow
 * chips, likes, seek bar with SponsorBlock marks, legend, settings, fullscreen).
 * It only reads and drives the player through {@link Host}; SmartTube's own engine and controls stay untouched.
 */
public final class QuestHud {
    /** What the HUD needs from the player. Implemented where the real PlaybackFragment is available. */
    public interface Host {
        long positionMs();
        long durationMs();
        boolean isPlaying();
        void togglePlay();
        void next();
        void seekTo(long positionMs);
        /** Short quality label such as "4K60" or "1080p"; empty when unknown. */
        String quality();
        /** Like / dislike counters as shown by YouTube; null when unknown. */
        String likes();
        String dislikes();
        boolean deArrowOn();
        void toggleDeArrow();
        void openOptions();
        void toggleFullscreen();
        /** Re-evaluates whether the HUD should be visible (controls shown, classic UI off...). */
        void syncVisibility();
        /** "4K • 16:9 • 60 FPS" for the VR player panel; empty when unknown. */
        String videoSummary();
        /** Current subtitle language (e.g. "Español"), or the "off" label. */
        String subtitleLabel();
        void openVideoSettings();
        void openSubtitles();
        void openScreenSettings();
        void like();
        void dislike();
        /** Makes the HUD (or nobody, with null) the place where SponsorBlock offers "Undo" after a skip. */
        void registerUndo(UndoTarget target);
    }

    /** Receives SponsorBlock's "Undo" offer; returns false to let SmartTube show its own dialog instead. */
    public interface UndoTarget { boolean offer(String label, long durationMs, Runnable undo); }

    // Bridges for the cinema controller's "Reproductor VR" panel (no-ops when no HUD is attached).
    static String videoSummary() { return active != null ? active.host.videoSummary() : ""; }
    static String subtitleLabel() { return active != null ? active.host.subtitleLabel() : null; }
    static void openVideoSettings() { if (active != null) active.host.openVideoSettings(); }
    static void openSubtitles() { if (active != null) active.host.openSubtitles(); }

    private static QuestHud active;
    private static final String PREFS = "quest_cinema_v1", CLASSIC = "classic_ui";

    /** True when the user chose SmartTube's original controls instead of the VR HUD. */
    public static boolean isClassic(Context c) { return c.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getBoolean(CLASSIC, false); }

    public static void setClassic(Context c, boolean classic) {
        c.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putBoolean(CLASSIC, classic).apply();
        if (active != null) active.host.syncVisibility();
    }

    /** True while a HUD is attached to the player (the cinema controller then hides its old opener button). */
    static boolean isActive() { return active != null; }

    /** Tag of the cinema controller's "Escenarios" button, replaced by the pill's menu button. */
    static final String OPENER_TAG = "quest_cinema_opener";

    /** Repaints the HUD with the colors of the current scenery theme. */
    static void refreshTheme() { if (active != null) active.restyle(); }

    private static final long TICK_MS = 500, STATUS_MS = 15_000;
    private final Context context;
    private final FrameLayout root;
    private final Host host;
    private final boolean spanish;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final View.OnLayoutChangeListener layoutListener;
    private final LinearLayout pill, bar;
    private final TextView device, wifiText, batteryText, time, quality, deArrow, likes, dislikes, rydNote, legend;
    private final View sepDevice, sepWifi, legendDot, likesBox, metaLogo;
    private final LinearLayout undoChip;
    private final TextView undoText;
    private Runnable undoAction;
    private long undoUntil;
    private final QuestUi.Icon wifiIcon, batteryIcon, playIcon;
    private final SegmentBar seek;
    private boolean shown, released;
    private long lastStatus;

    private final Runnable tick = new Runnable() {
        @Override public void run() {
            if (released || !shown) return;
            update();
            if (shown && !released) handler.postDelayed(this, TICK_MS);
        }
    };

    public QuestHud(FrameLayout root, Host host) {
        this.root = root;
        this.host = host;
        this.context = root.getContext();
        this.spanish = Locale.getDefault().getLanguage().equals("es");
        active = this;

        // ---- status pill (top left)
        pill = new LinearLayout(context);
        pill.setGravity(Gravity.CENTER_VERTICAL);
        pill.setPadding(dp(18), dp(6), dp(8), dp(6));
        pill.setBackground(QuestUi.box(context, QuestUi.GLASS, 28, 1.5f, QuestUi.border));
        metaLogo = new QuestUi.Icon(context, "meta", Color.WHITE);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(dp(30), dp(26));
        lp.rightMargin = dp(4);
        pill.addView(metaLogo, lp);
        device = text("", 16, Color.WHITE);
        pill.addView(device);
        sepDevice = separator();
        pill.addView(sepDevice);
        wifiIcon = new QuestUi.Icon(context, "wifi", Color.WHITE);
        pill.addView(wifiIcon, new LinearLayout.LayoutParams(dp(26), dp(26)));
        wifiText = text("", 16, Color.WHITE);
        pill.addView(wifiText);
        sepWifi = separator();
        pill.addView(sepWifi);
        batteryIcon = new QuestUi.Icon(context, "battery", Color.WHITE);
        pill.addView(batteryIcon, new LinearLayout.LayoutParams(dp(30), dp(26)));
        batteryText = text("", 16, Color.WHITE);
        pill.addView(batteryText);
        QuestUi.Icon menu = new QuestUi.Icon(context, "menu", Color.WHITE);
        menu.setFocusable(true);
        menu.setClickable(true);
        menu.setContentDescription(spanish ? "Menú del reproductor" : "Player menu");
        menu.setBackground(QuestUi.stateBg(context, Color.TRANSPARENT, QuestUi.FILL_HOVER, 18, false));
        menu.setOnClickListener(v -> host.openOptions());
        LinearLayout.LayoutParams mp = new LinearLayout.LayoutParams(dp(42), dp(38));
        mp.leftMargin = dp(10);
        pill.addView(menu, mp);
        FrameLayout.LayoutParams pillParams = new FrameLayout.LayoutParams(-2, dp(52), Gravity.TOP | Gravity.START);
        pillParams.setMargins(dp(16), dp(14), dp(16), 0);
        root.addView(pill, pillParams);

        // ---- control bar (bottom)
        bar = new LinearLayout(context);
        bar.setOrientation(LinearLayout.VERTICAL);
        bar.setPadding(dp(24), dp(10), dp(24), dp(8));
        bar.setBackground(QuestUi.box(context, QuestUi.GLASS, 26, 1.5f, QuestUi.border));

        LinearLayout row1 = new LinearLayout(context);
        row1.setGravity(Gravity.CENTER_VERTICAL);
        playIcon = new QuestUi.Icon(context, "play", Color.WHITE);
        row1.addView(iconButton(playIcon, spanish ? "Reproducir o pausar" : "Play or pause", host::togglePlay), new LinearLayout.LayoutParams(dp(58), dp(52)));
        row1.addView(iconButton(new QuestUi.Icon(context, "next", Color.WHITE), spanish ? "Siguiente" : "Next", host::next), new LinearLayout.LayoutParams(dp(58), dp(52)));
        time = text("0:00 / 0:00", 17, Color.WHITE);
        time.setPadding(dp(14), 0, dp(14), 0);
        row1.addView(time);
        View grow = new View(context);
        row1.addView(grow, new LinearLayout.LayoutParams(0, 1, 1f));
        // "Deshacer salto 5s": shown for a few seconds after SponsorBlock skips a segment.
        undoChip = new LinearLayout(context);
        undoChip.setGravity(Gravity.CENTER_VERTICAL);
        undoChip.setPadding(dp(12), dp(4), dp(18), dp(4));
        undoChip.setBackground(QuestUi.stateBg(context, 0x14ffffff, QuestUi.FILL_HOVER, 16, true));
        undoChip.setFocusable(true);
        undoChip.setClickable(true);
        undoChip.addView(new QuestUi.Icon(context, "undo", Color.WHITE), new LinearLayout.LayoutParams(dp(30), dp(30)));
        undoText = text("", 14, Color.WHITE);
        undoText.setPadding(dp(8), 0, 0, 0);
        undoChip.addView(undoText);
        undoChip.setOnClickListener(v -> {
            Runnable undo = undoAction;
            hideUndo();
            if (undo != null) undo.run();
        });
        undoChip.setVisibility(View.GONE);
        row1.addView(undoChip, new LinearLayout.LayoutParams(-2, dp(52)));
        quality = chip("", false, null);
        row1.addView(quality, chipParams());
        deArrow = chip("DeArrow", host.deArrowOn(), null);
        deArrow.setFocusable(true);
        deArrow.setClickable(true);
        deArrow.setOnClickListener(v -> { host.toggleDeArrow(); showDeArrow(); });
        row1.addView(deArrow, chipParams());
        LinearLayout likesColumn = new LinearLayout(context);
        likesBox = likesColumn;
        likesColumn.setOrientation(LinearLayout.VERTICAL);
        likesColumn.setPadding(dp(16), 0, 0, 0);
        LinearLayout counts = new LinearLayout(context);
        counts.setGravity(Gravity.CENTER_VERTICAL);
        counts.addView(iconButton(new QuestUi.Icon(context, "thumb_up", Color.WHITE), spanish ? "Me gusta" : "Like", host::like), new LinearLayout.LayoutParams(dp(30), dp(30)));
        likes = text("", 15, Color.WHITE);
        likes.setPadding(dp(4), 0, dp(12), 0);
        counts.addView(likes);
        counts.addView(iconButton(new QuestUi.Icon(context, "thumb_down", Color.WHITE), spanish ? "No me gusta" : "Dislike", host::dislike), new LinearLayout.LayoutParams(dp(30), dp(30)));
        dislikes = text("", 15, Color.WHITE);
        dislikes.setPadding(dp(4), 0, 0, 0);
        counts.addView(dislikes);
        likesColumn.addView(counts);
        rydNote = text("Return YouTube Dislike", 11, QuestUi.MUTED);
        likesColumn.addView(rydNote);
        row1.addView(likesColumn);
        bar.addView(row1, new LinearLayout.LayoutParams(-1, -2));

        seek = new SegmentBar(context);
        bar.addView(seek, new LinearLayout.LayoutParams(-1, dp(40)));

        LinearLayout row3 = new LinearLayout(context);
        row3.setGravity(Gravity.CENTER_VERTICAL);
        View lead = new View(context);
        row3.addView(lead, new LinearLayout.LayoutParams(0, 1, 1.15f));
        legendDot = new View(context);
        legendDot.setBackground(QuestUi.box(context, 0xff7ac043, 6, 0, 0));
        row3.addView(legendDot, new LinearLayout.LayoutParams(dp(12), dp(12)));
        legend = text(spanish ? "Segmentos de SponsorBlock" : "SponsorBlock segments", 13, Color.WHITE);
        legend.setPadding(dp(8), 0, 0, 0);
        row3.addView(legend);
        View grow2 = new View(context);
        row3.addView(grow2, new LinearLayout.LayoutParams(0, 1, .55f));
        row3.addView(iconButton(new QuestUi.Icon(context, "gear", Color.WHITE), spanish ? "Pantalla virtual" : "Virtual screen", host::openScreenSettings), new LinearLayout.LayoutParams(dp(40), dp(34)));
        row3.addView(iconButton(new QuestUi.Icon(context, "expand", Color.WHITE), spanish ? "Pantalla completa" : "Fullscreen", host::toggleFullscreen), new LinearLayout.LayoutParams(dp(40), dp(34)));
        // The SponsorBlock marks are drawn by the seek bar just under it; this row sits on that same line.
        LinearLayout.LayoutParams r3 = new LinearLayout.LayoutParams(-1, dp(34));
        r3.topMargin = -dp(22);
        bar.addView(row3, r3);

        FrameLayout.LayoutParams barParams = new FrameLayout.LayoutParams(dp(900), -2, Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL);
        barParams.bottomMargin = dp(28);
        root.addView(bar, barParams);
        layoutListener = (v, l, t, r, b, ol, ot, or, ob) -> { if (r - l != or - ol) fitWidth(); };
        root.addOnLayoutChangeListener(layoutListener);
        pill.setVisibility(View.GONE);
        bar.setVisibility(View.GONE);
        setSegments(null);
        host.registerUndo(this::offerUndo);
        // The pill's menu button replaces the cinema controller's "Escenarios" button.
        View opener = root.findViewWithTag(OPENER_TAG);
        if (opener != null) opener.setVisibility(View.GONE);
        refreshStatus();
    }

    private int dp(float n) { return QuestUi.dp(context, n); }

    private TextView text(String t, float sp, int color) { return QuestUi.label(context, t, sp, color); }

    private View separator() {
        View v = new View(context);
        v.setBackgroundColor(0x55ffffff);
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(Math.max(1, dp(1)), dp(24));
        p.setMargins(dp(12), 0, dp(12), 0);
        v.setLayoutParams(p);
        return v;
    }

    private View iconButton(View icon, String description, Runnable action) {
        icon.setFocusable(true);
        icon.setClickable(true);
        icon.setContentDescription(description);
        icon.setBackground(QuestUi.stateBg(context, Color.TRANSPARENT, QuestUi.FILL_HOVER, 16, false));
        icon.setOnClickListener(v -> action.run());
        return icon;
    }

    private TextView chip(String label, boolean selected, Runnable onClick) {
        TextView t = QuestUi.chip(context, label, false, onClick == null ? () -> {} : onClick);
        t.setTextSize(15);
        t.setPadding(dp(16), 0, dp(16), 0);
        t.setGravity(Gravity.CENTER);
        android.graphics.drawable.StateListDrawable bg = new android.graphics.drawable.StateListDrawable();
        bg.addState(new int[]{android.R.attr.state_pressed}, QuestUi.box(context, QuestUi.FILL_HOVER, 20, 1, QuestUi.border));
        bg.addState(new int[]{android.R.attr.state_focused}, QuestUi.box(context, QuestUi.FILL_HOVER, 20, 2, QuestUi.accent));
        bg.addState(new int[]{android.R.attr.state_hovered}, QuestUi.box(context, QuestUi.FILL_HOVER, 20, 1, QuestUi.border));
        bg.addState(new int[]{}, QuestUi.box(context, 0x14ffffff, 20, 1, 0x29ffffff));
        t.setBackground(bg);
        t.setFocusable(onClick != null);
        t.setClickable(onClick != null);
        return t;
    }

    /** DeArrow on: white label; off: muted. The chip itself stays neutral like in the mockup. */
    private void showDeArrow() {
        deArrow.setTextColor(host.deArrowOn() ? Color.WHITE : QuestUi.MUTED);
    }

    private LinearLayout.LayoutParams chipParams() {
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-2, dp(40));
        p.leftMargin = dp(8);
        return p;
    }

    private void fitWidth() {
        FrameLayout.LayoutParams p = (FrameLayout.LayoutParams) bar.getLayoutParams();
        int width = Math.min(dp(840), Math.max(dp(360), Math.round(root.getWidth() * .70f)));
        if (p.width != width) { p.width = width; bar.setLayoutParams(p); }
    }

    // ------------------------------------------------------------------ state coming from the player

    /** Called when SmartTube shows or hides its controls overlay. */
    public void setControlsShown(boolean show) {
        if (released || shown == show) return;
        shown = show;
        pill.setVisibility(show ? View.VISIBLE : View.GONE);
        bar.setVisibility(show ? View.VISIBLE : View.GONE);
        handler.removeCallbacks(tick);
        if (show) {
            fitWidth();
            refreshStatus();
            showDeArrow();
            update();
            handler.postDelayed(tick, TICK_MS);
        }
    }

    /** SponsorBlock segments as {startFraction, endFraction, ARGB color}. */
    public void setSegments(List<float[]> segments) {
        seek.segments = segments == null ? new ArrayList<float[]>() : new ArrayList<>(segments);
        int legendVisibility = seek.segments.isEmpty() ? View.INVISIBLE : View.VISIBLE;
        legend.setVisibility(legendVisibility);
        legendDot.setVisibility(legendVisibility);
        seek.invalidate();
    }

    private void restyle() {
        pill.setBackground(QuestUi.box(context, QuestUi.GLASS, 28, 1.5f, QuestUi.border));
        bar.setBackground(QuestUi.box(context, QuestUi.GLASS, 26, 1.5f, QuestUi.border));
        seek.invalidate();
    }

    public void release() {
        released = true;
        if (active == this) active = null;
        host.registerUndo(null);
        handler.removeCallbacksAndMessages(null);
        root.removeOnLayoutChangeListener(layoutListener);
        root.removeView(pill);
        root.removeView(bar);
    }

    private void update() {
        host.syncVisibility();
        if (!shown) return;
        long pos = Math.max(0, host.positionMs()), dur = Math.max(0, host.durationMs());
        time.setText(fmt(pos) + " / " + fmt(dur));
        playIcon.setKind(host.isPlaying() ? "pause" : "play");
        if (!seek.dragging) seek.progress = dur > 0 ? Math.min(1f, pos / (float) dur) : 0f;
        seek.invalidate();
        String q = host.quality();
        quality.setText(q == null ? "" : q);
        quality.setVisibility(q == null || q.isEmpty() ? View.GONE : View.VISIBLE);
        String l = host.likes(), d = host.dislikes();
        likesBox.setVisibility(l == null && d == null ? View.GONE : View.VISIBLE);
        likes.setText(l == null ? "" : l);
        dislikes.setText(d == null ? "" : d);
        rydNote.setVisibility(d == null ? View.GONE : View.VISIBLE);
        updateUndo();
        if (System.currentTimeMillis() - lastStatus > STATUS_MS) refreshStatus();
    }

    /** SponsorBlock skipped a segment: offer "Deshacer salto" in the bar while it is visible. */
    private boolean offerUndo(String label, long durationMs, Runnable undo) {
        if (released || !shown) return false; // not visible: SmartTube shows its own Undo dialog
        undoAction = undo;
        undoUntil = System.currentTimeMillis() + durationMs;
        undoChip.setContentDescription(label);
        undoChip.setVisibility(View.VISIBLE);
        updateUndo();
        return true;
    }

    private void updateUndo() {
        if (undoChip.getVisibility() != View.VISIBLE) return;
        long left = undoUntil - System.currentTimeMillis();
        if (left <= 0) { hideUndo(); return; }
        long seconds = (left + 999) / 1000;
        undoText.setText((spanish ? "Deshacer salto" : "Undo skip") + "\n" + seconds + "s");
    }

    private void hideUndo() {
        undoAction = null;
        undoChip.setVisibility(View.GONE);
    }

    private static String fmt(long ms) {
        long s = ms / 1000, h = s / 3600, m = (s / 60) % 60;
        return h > 0 ? String.format(Locale.US, "%d:%02d:%02d", h, m, s % 60) : String.format(Locale.US, "%d:%02d", m, s % 60);
    }

    // ------------------------------------------------------------------ device status (real values)

    private void refreshStatus() {
        lastStatus = System.currentTimeMillis();
        String model = Build.MODEL == null ? "" : Build.MODEL;
        boolean quest = model.toLowerCase(Locale.US).contains("quest");
        device.setText(quest ? (model.startsWith("Meta") ? model : "Meta " + model) : model);
        metaLogo.setVisibility(quest ? View.VISIBLE : View.GONE);
        int level = batteryPercent();
        batteryText.setText(level >= 0 ? level + "%" : "");
        batteryIcon.setLevel(Math.max(level, 0));
        batteryIcon.setVisibility(level >= 0 ? View.VISIBLE : View.GONE);
        int bars = wifiBars();
        wifiIcon.setVisibility(bars >= 0 ? View.VISIBLE : View.GONE);
        wifiText.setVisibility(bars >= 0 ? View.VISIBLE : View.GONE);
        if (bars >= 0) {
            wifiIcon.setLevel(bars * 25);
            wifiText.setText(wifiLabel(bars));
        }
        // Separators only between items that are actually shown.
        sepDevice.setVisibility(bars >= 0 || level >= 0 ? View.VISIBLE : View.GONE);
        sepWifi.setVisibility(bars >= 0 && level >= 0 ? View.VISIBLE : View.GONE);
    }

    private int batteryPercent() {
        try {
            Intent i = context.registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
            // TVs and other mains-powered devices report no battery: show nothing instead of "0%".
            if (i == null || !i.getBooleanExtra(BatteryManager.EXTRA_PRESENT, true)) return -1;
            int level = i.getIntExtra(BatteryManager.EXTRA_LEVEL, -1), scale = i.getIntExtra(BatteryManager.EXTRA_SCALE, 100);
            return level < 0 || scale <= 0 ? -1 : Math.round(level * 100f / scale);
        } catch (RuntimeException e) {
            return -1;
        }
    }

    /** 0-4 Wi-Fi signal bars, or -1 when Wi-Fi is not readable (no permission / not connected). */
    @SuppressWarnings("deprecation")
    private int wifiBars() {
        try {
            WifiManager wm = (WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
            if (wm == null || !wm.isWifiEnabled()) return -1;
            WifiInfo info = wm.getConnectionInfo();
            if (info == null || info.getNetworkId() == -1) return -1;
            return WifiManager.calculateSignalLevel(info.getRssi(), 5);
        } catch (RuntimeException e) {
            return -1;
        }
    }

    private String wifiLabel(int bars) {
        if (bars >= 4) return spanish ? "Excelente" : "Excellent";
        if (bars == 3) return spanish ? "Buena" : "Good";
        if (bars == 2) return spanish ? "Regular" : "Fair";
        return spanish ? "Débil" : "Weak";
    }

    // ------------------------------------------------------------------ seek bar with SponsorBlock marks

    private final class SegmentBar extends View {
        float progress;
        boolean dragging;
        List<float[]> segments = new ArrayList<>();
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final RectF rect = new RectF();

        SegmentBar(Context c) {
            super(c);
            setFocusable(false);
            setContentDescription(spanish ? "Barra de progreso" : "Seek bar");
        }

        @Override protected void onDraw(Canvas g) {
            float w = getWidth(), trackH = dp(8), r = trackH / 2f;
            paint.setColor(QuestUi.TRACK);
            rect.set(0, 0, w, trackH);
            g.drawRoundRect(rect, r, r, paint);
            // Each SponsorBlock segment colors its stretch of the bar (sponsors yellow), as in the mockup.
            for (float[] seg : segments) {
                paint.setColor((int) seg[2] == 0 ? 0xffffd23f : (int) seg[2]);
                rect.set(w * seg[0], 0, Math.max(w * seg[0] + trackH, w * seg[1]), trackH);
                g.drawRoundRect(rect, r, r, paint);
            }
            paint.setColor(QuestUi.progress);
            rect.set(0, 0, Math.max(trackH, w * progress), trackH);
            g.drawRoundRect(rect, r, r, paint);
            // One small mark under the bar at the start of each SponsorBlock segment, like the mockup.
            float markW = dp(26), markH = dp(8), top = trackH + dp(10);
            for (float[] s : segments) {
                paint.setColor((int) s[2] == 0 ? 0xffffd23f : (int) s[2]);
                float x = Math.max(0, Math.min(w - markW, w * s[0] - markW / 2f));
                rect.set(x, top, x + markW, top + markH);
                g.drawRoundRect(rect, dp(2), dp(2), paint);
            }
        }

        @Override public boolean onTouchEvent(MotionEvent e) {
            float f = Math.max(0f, Math.min(1f, e.getX() / Math.max(1, getWidth())));
            switch (e.getActionMasked()) {
                case MotionEvent.ACTION_DOWN:
                case MotionEvent.ACTION_MOVE:
                    dragging = true; progress = f; invalidate();
                    getParent().requestDisallowInterceptTouchEvent(true);
                    return true;
                case MotionEvent.ACTION_UP:
                    dragging = false; progress = f; invalidate();
                    long dur = host.durationMs();
                    if (dur > 0) host.seekTo((long) (dur * f));
                    return true;
                case MotionEvent.ACTION_CANCEL:
                    dragging = false;
                    return true;
                default:
                    return super.onTouchEvent(e);
            }
        }
    }
}
