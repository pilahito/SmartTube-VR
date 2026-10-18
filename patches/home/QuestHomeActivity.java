package com.liskovsoft.smartyoutubetv2.tv.ui.home;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.StateListDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.text.InputType;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import com.liskovsoft.smartyoutubetv2.tv.ui.main.SplashActivity;
import com.liskovsoft.smartyoutubetv2.tv.ui.playback.cinema.CinemaGeometry;
import com.liskovsoft.smartyoutubetv2.tv.ui.playback.cinema.CinemaSceneView;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * Quest-friendly launcher drawn over the selected cinema scenery.
 * Everything is reachable with the controller/D-pad: OK opens a tile, long press or Y/Menu edits it.
 * The original SmartTube browse UI is untouched and opened by the YouTube tile.
 */
public final class QuestHomeActivity extends Activity {
    private FrameLayout root;
    private LinearLayout content;
    private TextView title, hint;
    private int laidOutW, laidOutH;
    private SharedPreferences prefs, cinema;
    private CinemaSceneView scenery;
    private LinearLayout grid;
    private final List<HomeTiles.Tile> tiles = new ArrayList<>();
    private boolean spanish;
    private AlertDialog dialog;
    private int focusIndex = -1; // -1 = YouTube tile

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        spanish = Locale.getDefault().getLanguage().equals("es");
        prefs = getSharedPreferences("quest_home_v1", MODE_PRIVATE);
        cinema = getSharedPreferences("quest_cinema_v1", MODE_PRIVATE);
        loadTiles();

        root = new FrameLayout(this);
        root.setBackgroundColor(0xff05070c);
        scenery = new CinemaSceneView(this);
        root.addView(scenery, new FrameLayout.LayoutParams(-1, -1));

        content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(32), dp(24), dp(32), dp(24));
        GradientDrawable panel = new GradientDrawable();
        panel.setColor(0xcc0d1521); panel.setCornerRadius(dp(24)); panel.setStroke(dp(2), 0xff2b3b52);
        content.setBackground(panel);
        title = new TextView(this);
        title.setText("SmartTube VR"); title.setTextColor(0xff83f5dc); title.setTextSize(28);
        content.addView(title);
        hint = new TextView(this);
        hint.setText(t("OK para abrir · mantén pulsado o pulsa Y/Menú para mover o quitar",
                "OK to open · long press or press Y/Menu to move or remove"));
        hint.setTextColor(0xffc2ccd9); hint.setTextSize(14); hint.setPadding(0, dp(4), 0, dp(14));
        content.addView(hint);
        grid = new LinearLayout(this);
        grid.setOrientation(LinearLayout.VERTICAL);
        grid.setGravity(Gravity.CENTER_HORIZONTAL);
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.addView(grid);
        content.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1f));

        FrameLayout.LayoutParams cp = new FrameLayout.LayoutParams(-1, -1, Gravity.CENTER);
        cp.setMargins(dp(16), dp(16), dp(16), dp(16));
        root.addView(content, cp);
        setContentView(root);
        // Responsive: re-flow the grid whenever the Quest panel / window is resized or rotated.
        root.addOnLayoutChangeListener((v, l, t, r, b, ol, ot, or, ob) -> {
            if (r - l != laidOutW || b - t != laidOutH) root.post(() -> { rememberFocus(); rebuild(); });
        });
        rebuild();
    }

    @Override protected void onResume() {
        super.onResume();
        applyScenery(); // The player menu may have changed the scenery meanwhile.
    }

    private void applyScenery() {
        int scene = Math.max(0, Math.min(CinemaGeometry.COUNT - 1, cinema.getInt("scene", 0)));
        scenery.configure(scene, cinema.getBoolean("eco", false),
                Math.max(55, cinema.getInt("brightness", 75)));
    }

    @Override protected void onDestroy() { dismiss(); super.onDestroy(); }

    private String t(String es, String en) { return spanish ? es : en; }
    private int dp(int n) { return (int) (n * getResources().getDisplayMetrics().density + .5f); }
    private void toast(String s) { Toast.makeText(this, s, Toast.LENGTH_LONG).show(); }
    private void dismiss() { if (dialog != null) { dialog.dismiss(); dialog = null; } }
    private void show(AlertDialog d) { dismiss(); dialog = d; d.show(); }

    private void loadTiles() {
        tiles.clear();
        if (!prefs.contains("tiles")) {
            // First run: pin Brave automatically when it is installed.
            if (launchIntent(HomeTiles.BRAVE) != null)
                HomeTiles.add(tiles, new HomeTiles.Tile(HomeTiles.APP, HomeTiles.BRAVE, appLabel(HomeTiles.BRAVE)));
            save();
        } else {
            tiles.addAll(HomeTiles.decode(prefs.getString("tiles", "")));
        }
    }

    private void save() { prefs.edit().putString("tiles", HomeTiles.encode(tiles)).apply(); }

    private Intent launchIntent(String pkg) {
        try { return getPackageManager().getLaunchIntentForPackage(pkg); }
        catch (RuntimeException e) { return null; }
    }

    private String appLabel(String pkg) {
        try {
            PackageManager pm = getPackageManager();
            return pm.getApplicationLabel(pm.getApplicationInfo(pkg, 0)).toString();
        } catch (Exception e) { return pkg; }
    }

    private Drawable appIcon(String pkg) {
        // Prefer the real launcher icon of the app's entry activity, then the application icon.
        Intent launch = pkg.equals(getPackageName()) ? null : launchIntent(pkg);
        if (launch != null) {
            try { return getPackageManager().getActivityIcon(launch); }
            catch (Exception ignored) { }
        }
        try { return getPackageManager().getApplicationIcon(pkg); }
        catch (Exception e) { return null; }
    }

    private GradientDrawable background(int color, int stroke) {
        GradientDrawable d = new GradientDrawable(); d.setColor(color); d.setCornerRadius(dp(18));
        d.setStroke(dp(3), stroke); return d;
    }

    private View tile(String label, Drawable icon, String glyph, boolean small, Runnable open, Runnable edit) {
        LinearLayout v = new LinearLayout(this);
        v.setOrientation(LinearLayout.VERTICAL);
        v.setGravity(Gravity.CENTER);
        int pad = Math.max(dp(6), tileSize / 16);
        v.setPadding(pad, pad, pad, pad);
        v.setFocusable(true); v.setClickable(true);
        StateListDrawable state = new StateListDrawable();
        state.addState(new int[]{android.R.attr.state_pressed}, background(0xff2f7b83, 0xffa0fff0));
        state.addState(new int[]{android.R.attr.state_focused}, background(0xff234f65, 0xff66f5df));
        state.addState(new int[]{}, background(0xe6182331, 0xff40546c));
        v.setBackground(state);
        if (icon != null) {
            ImageView image = new ImageView(this);
            image.setImageDrawable(icon);
            image.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
            image.setScaleType(ImageView.ScaleType.FIT_CENTER);
            int iconPx = Math.round(tileSize * (small ? .22f : .42f));
            v.addView(image, new LinearLayout.LayoutParams(iconPx, iconPx));
        } else {
            TextView g = new TextView(this);
            g.setText(glyph); g.setTextSize(TypedValue.COMPLEX_UNIT_PX, tileSize * (small ? .17f : .26f)); g.setTextColor(0xff83f5dc); g.setGravity(Gravity.CENTER);
            g.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
            v.addView(g);
        }
        TextView text = new TextView(this);
        text.setText(label); text.setTextColor(Color.WHITE);
        text.setTextSize(TypedValue.COMPLEX_UNIT_PX, Math.max(dp(12), tileSize * (small ? .085f : .1f)));
        text.setEllipsize(android.text.TextUtils.TruncateAt.END);
        text.setGravity(Gravity.CENTER); text.setMaxLines(small ? 1 : 2); text.setPadding(0, dp(6), 0, 0);
        v.addView(text);
        v.setContentDescription(label);
        v.setOnClickListener(x -> open.run());
        v.setOnFocusChangeListener((x, focused) -> x.animate().scaleX(focused ? 1.06f : 1f).scaleY(focused ? 1.06f : 1f).setDuration(120).start());
        if (edit != null) {
            v.setLongClickable(true);
            v.setOnLongClickListener(x -> { edit.run(); return true; });
            v.setOnKeyListener((x, code, e) -> {
                if (code != KeyEvent.KEYCODE_MENU && code != KeyEvent.KEYCODE_BUTTON_Y) return false;
                if (e.getAction() == KeyEvent.ACTION_DOWN && e.getRepeatCount() == 0) edit.run();
                return true;
            });
        }
        return v;
    }

    private int columns = 4, tileSize;

    private void rememberFocus() {
        View focused = getCurrentFocus();
        if (focused != null && focused.getTag() instanceof Integer) focusIndex = (Integer) focused.getTag();
    }

    private void rebuild() {
        laidOutW = root.getWidth(); laidOutH = root.getHeight();
        int w = laidOutW > 0 ? laidOutW : getResources().getDisplayMetrics().widthPixels;
        int h = laidOutH > 0 ? laidOutH : getResources().getDisplayMetrics().heightPixels;
        float density = getResources().getDisplayMetrics().density;
        int[] geometry = HomeTiles.layoutFor(w, h, density);
        columns = geometry[0]; tileSize = geometry[1];
        title.setTextSize(TypedValue.COMPLEX_UNIT_PX, Math.max(dp(20), tileSize * .16f));
        hint.setTextSize(TypedValue.COMPLEX_UNIT_PX, Math.max(dp(12), tileSize * .075f));
        grid.removeAllViews();
        List<View> main = new ArrayList<>();
        main.add(tile("YouTube", appIcon(getPackageName()), "▶", false, this::openYouTube, null));
        main.get(0).setTag(-1);
        for (int i = 0; i < tiles.size(); i++) {
            final int index = i;
            HomeTiles.Tile tile = tiles.get(i);
            Drawable icon = tile.isApp() ? appIcon(tile.target) : null;
            View view = tile(tile.label, icon, "🌐", false, () -> open(tile), () -> editTile(index));
            view.setTag(index);
            main.add(view);
        }
        addRows(main, tileSize, Math.round(tileSize * .85f));
        List<View> actions = new ArrayList<>();
        actions.add(tile(t("Añadir app", "Add app"), null, "+", true, this::addApp, null));
        actions.add(tile(t("Añadir web", "Add website"), null, "🔗", true, this::addWeb, null));
        actions.add(tile(t("Organizar", "Organize"), null, "⇄", true, this::organize, null));
        actions.add(tile(t("Escenario", "Scenery"), null, "🌲", true, this::chooseScene, null));
        addRows(actions, tileSize, Math.round(tileSize * .5f));
        int focus = Math.min(focusIndex + 1, main.size() - 1);
        main.get(Math.max(0, focus)).requestFocus();
    }

    private void addRows(List<View> views, int width, int height) {
        LinearLayout row = null;
        for (int i = 0; i < views.size(); i++) {
            if (i % columns == 0) {
                row = new LinearLayout(this);
                row.setOrientation(LinearLayout.HORIZONTAL);
                grid.addView(row, new LinearLayout.LayoutParams(-2, -2));
            }
            LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(width, height);
            p.setMargins(dp(8), dp(8), dp(8), dp(8));
            row.addView(views.get(i), p);
        }
    }

    private void openYouTube() {
        focusIndex = -1;
        try {
            startActivity(new Intent(this, SplashActivity.class).setAction(Intent.ACTION_MAIN));
        } catch (RuntimeException e) {
            toast(t("No se pudo abrir YouTube", "Could not open YouTube"));
        }
    }

    private void open(HomeTiles.Tile tile) {
        focusIndex = tiles.indexOf(tile);
        if (tile.isApp()) {
            Intent intent = launchIntent(tile.target);
            if (intent == null) {
                toast(t("Esta app ya no está instalada. Pulsa Y para quitarla.", "This app is no longer installed. Press Y to remove it."));
                return;
            }
            try { startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)); }
            catch (RuntimeException e) { toast(t("No se pudo abrir la app", "Could not open the app")); }
        } else {
            openUrl(tile.target);
        }
    }

    /** Opens a web shortcut in Brave when installed, otherwise in the default browser. */
    private void openUrl(String url) {
        Intent view = new Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        if (launchIntent(HomeTiles.BRAVE) != null) {
            try { startActivity(new Intent(view).setPackage(HomeTiles.BRAVE)); return; }
            catch (ActivityNotFoundException ignored) { }
        }
        try { startActivity(view); }
        catch (ActivityNotFoundException e) { toast(t("No hay ningún navegador instalado", "No browser is installed")); }
    }

    private void editTile(int index) {
        if (index < 0 || index >= tiles.size()) return;
        String[] labels = {t("Mover a la izquierda", "Move left"), t("Mover a la derecha", "Move right"),
                t("Quitar del inicio", "Remove from home")};
        show(new AlertDialog.Builder(this).setTitle(tiles.get(index).label).setItems(labels, (d, which) -> {
            if (which == 2) {
                tiles.remove(index);
                focusIndex = Math.min(index, tiles.size() - 1);
            } else {
                focusIndex = HomeTiles.move(tiles, index, which == 0 ? -1 : 1);
            }
            save(); rebuild();
        }).setNegativeButton(t("Volver", "Back"), null).create());
    }

    private void organize() {
        if (tiles.isEmpty()) { toast(t("Aún no has añadido apps ni webs", "You have not added apps or websites yet")); return; }
        String[] labels = new String[tiles.size()];
        for (int i = 0; i < labels.length; i++) labels[i] = (i + 1) + ". " + tiles.get(i).label;
        show(new AlertDialog.Builder(this).setTitle(t("Organizar inicio", "Organize home"))
                .setItems(labels, (d, which) -> editTile(which))
                .setNegativeButton(t("Volver", "Back"), null).create());
    }

    private void addApp() {
        if (tiles.size() >= HomeTiles.MAX_TILES) { toast(t("El inicio está lleno", "Home is full")); return; }
        PackageManager pm = getPackageManager();
        Intent query = new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER);
        final List<String[]> apps = new ArrayList<>(); // {label, package}
        try {
            for (ResolveInfo info : pm.queryIntentActivities(query, 0)) {
                String pkg = info.activityInfo.packageName;
                if (pkg.equals(getPackageName()) || HomeTiles.indexOfApp(tiles, pkg) >= 0) continue;
                boolean seen = false;
                for (String[] a : apps) if (a[1].equals(pkg)) seen = true;
                if (!seen) apps.add(new String[]{String.valueOf(info.loadLabel(pm)), pkg});
            }
        } catch (RuntimeException e) { /* fall through to empty list */ }
        if (apps.isEmpty()) { toast(t("No hay más apps para añadir", "No more apps to add")); return; }
        Collections.sort(apps, (a, b) -> a[0].compareToIgnoreCase(b[0]));
        String[] labels = new String[apps.size()];
        for (int i = 0; i < labels.length; i++) labels[i] = apps.get(i)[0];
        show(new AlertDialog.Builder(this).setTitle(t("Añadir app al inicio", "Add app to home"))
                .setItems(labels, (d, which) -> {
                    if (HomeTiles.add(tiles, new HomeTiles.Tile(HomeTiles.APP, apps.get(which)[1], apps.get(which)[0]))) {
                        focusIndex = tiles.size() - 1; save(); rebuild();
                    }
                }).setNegativeButton(t("Volver", "Back"), null).create());
    }

    private void addWeb() {
        if (tiles.size() >= HomeTiles.MAX_TILES) { toast(t("El inicio está lleno", "Home is full")); return; }
        LinearLayout form = new LinearLayout(this);
        form.setOrientation(LinearLayout.VERTICAL);
        form.setPadding(dp(20), dp(8), dp(20), 0);
        EditText name = new EditText(this);
        name.setHint(t("Nombre (p. ej. Mis series)", "Name (e.g. My shows)"));
        name.setSingleLine(true);
        EditText url = new EditText(this);
        url.setHint("https://…");
        url.setSingleLine(true);
        url.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_URI);
        form.addView(name); form.addView(url);
        AlertDialog d = new AlertDialog.Builder(this).setTitle(t("Añadir web", "Add website")).setView(form)
                .setMessage(t("Se abrirá en Brave si está instalado; si no, en el navegador predeterminado.",
                        "Opens in Brave when installed, otherwise in the default browser."))
                .setPositiveButton(t("Añadir", "Add"), null)
                .setNegativeButton(t("Cancelar", "Cancel"), null).create();
        show(d);
        d.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String target = HomeTiles.normalizeUrl(url.getText().toString());
            if (target == null) { url.setError(t("Dirección no válida", "Invalid address")); return; }
            String label = name.getText().toString().trim();
            if (label.isEmpty()) label = Uri.parse(target).getHost();
            if (HomeTiles.add(tiles, new HomeTiles.Tile(HomeTiles.WEB, target, label))) {
                focusIndex = tiles.size() - 1; save(); rebuild();
            }
            dismiss();
        });
    }

    private void chooseScene() {
        int current = Math.max(0, Math.min(CinemaGeometry.COUNT - 1, cinema.getInt("scene", 0)));
        show(new AlertDialog.Builder(this).setTitle(t("Elegir escenario", "Choose scenery"))
                .setSingleChoiceItems(CinemaGeometry.names(spanish), current, (d, which) -> {
                    cinema.edit().putInt("scene", which).putBoolean("enabled", true).apply();
                    applyScenery();
                    dismiss();
                }).setNegativeButton(t("Volver", "Back"), null).create());
    }
}
