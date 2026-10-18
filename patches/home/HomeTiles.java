package com.liskovsoft.smartyoutubetv2.tv.ui.home;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Pure-Java model of the pinned tiles, so persistence and ordering are testable without Android. */
public final class HomeTiles {
    public static final String APP = "app", WEB = "web";
    public static final int MAX_TILES = 24, MAX_LABEL = 40;
    public static final String BRAVE = "com.brave.browser";

    public static final class Tile {
        public final String type, target, label;
        public Tile(String type, String target, String label) {
            this.type = type; this.target = target; this.label = cleanLabel(label, target);
        }
        public boolean isApp() { return APP.equals(type); }
    }

    private HomeTiles() { }

    static String cleanLabel(String label, String fallback) {
        String value = label == null ? "" : label.replaceAll("[\\t\\r\\n]+", " ").trim();
        if (value.isEmpty()) value = fallback == null ? "" : fallback;
        return value.length() > MAX_LABEL ? value.substring(0, MAX_LABEL) : value;
    }

    /** Returns a safe http(s) URL, adding https:// when no scheme was typed, or null when invalid. */
    public static String normalizeUrl(String raw) {
        if (raw == null) return null;
        String value = raw.trim();
        if (value.isEmpty() || value.matches(".*\\s.*")) return null;
        if (!value.matches("(?i)^[a-z][a-z0-9+.-]*://.*")) value = "https://" + value;
        try {
            URI uri = new URI(value);
            String scheme = uri.getScheme() == null ? "" : uri.getScheme().toLowerCase(Locale.ROOT);
            if (!scheme.equals("http") && !scheme.equals("https")) return null;
            String host = uri.getHost();
            if (host == null || host.isEmpty() || !host.contains(".") && !host.equals("localhost")) return null;
            return value;
        } catch (Exception e) {
            return null;
        }
    }

    public static boolean isValidPackage(String pkg) {
        return pkg != null && pkg.matches("[A-Za-z][A-Za-z0-9_]*(\\.[A-Za-z0-9_]+)+");
    }

    public static List<Tile> decode(String stored) {
        List<Tile> tiles = new ArrayList<>();
        if (stored == null) return tiles;
        for (String line : stored.split("\n")) {
            String[] f = line.split("\t", -1);
            if (f.length != 3 || tiles.size() >= MAX_TILES) continue;
            if (APP.equals(f[0]) && isValidPackage(f[1]) && indexOfApp(tiles, f[1]) < 0) {
                tiles.add(new Tile(APP, f[1], f[2]));
            } else if (WEB.equals(f[0]) && normalizeUrl(f[1]) != null) {
                tiles.add(new Tile(WEB, normalizeUrl(f[1]), f[2]));
            }
        }
        return tiles;
    }

    public static String encode(List<Tile> tiles) {
        StringBuilder out = new StringBuilder();
        for (Tile t : tiles) {
            if (out.length() > 0) out.append('\n');
            out.append(t.type).append('\t').append(t.target).append('\t').append(t.label);
        }
        return out.toString();
    }

    public static int indexOfApp(List<Tile> tiles, String pkg) {
        for (int i = 0; i < tiles.size(); i++)
            if (tiles.get(i).isApp() && tiles.get(i).target.equals(pkg)) return i;
        return -1;
    }

    /** Adds a tile; returns false when it is a duplicate app, invalid, or the home is full. */
    public static boolean add(List<Tile> tiles, Tile tile) {
        if (tiles.size() >= MAX_TILES) return false;
        if (tile.isApp() ? !isValidPackage(tile.target) || indexOfApp(tiles, tile.target) >= 0
                : !WEB.equals(tile.type) || normalizeUrl(tile.target) == null) return false;
        tiles.add(tile);
        return true;
    }

    /** Tile geometry for the current window: {columns, tile size in px}, any aspect ratio. */
    public static int[] layoutFor(int widthPx, int heightPx, float density) {
        if (density <= 0) density = 1;
        float wDp = Math.max(200, widthPx / density), hDp = Math.max(200, heightPx / density);
        boolean portrait = hDp > wDp * 1.1f;
        float usable = wDp - 96; // window margins + panel padding
        int columns = Math.max(2, Math.min(portrait ? 4 : 7, Math.round(usable / (portrait ? 170f : 200f))));
        float widthFit = usable / columns - 16; // 8dp margin each side of a tile
        // Keep roughly two (landscape) or three (portrait) rows of main tiles on screen; the grid scrolls beyond.
        float heightFit = (hDp - 150) / (portrait ? 3.2f : 2.1f) / .85f;
        float tile = Math.min(300, Math.min(widthFit, Math.max(72, heightFit)));
        // When the height made tiles smaller, use the spare width for extra columns.
        columns = Math.max(columns, Math.min(8, (int) Math.floor(usable / (tile + 16))));
        tile = Math.max(48, Math.min(tile, usable / columns - 16));
        return new int[]{columns, (int) Math.floor(tile * density)};
    }

    /** Moves the tile by delta positions; returns its new index (unchanged at the edges). */
    public static int move(List<Tile> tiles, int index, int delta) {
        if (index < 0 || index >= tiles.size()) return index;
        int target = Math.max(0, Math.min(tiles.size() - 1, index + delta));
        if (target != index) tiles.add(target, tiles.remove(index));
        return target;
    }
}
