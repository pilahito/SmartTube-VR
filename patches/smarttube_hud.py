#!/usr/bin/env python3
"""Connect the VR player HUD (QuestHud, docs/interfaz/reproductor.jpg) to SmartTube's playback fragment.

Must run after smarttube_cinema.py, which copies QuestHud/QuestTheme and the cinema controller, and after
smarttube_quest_media.py, which adds SponsorBlock's "undo skip" (shown as a chip in the HUD while it is visible).
The HUD only reads the player through QuestHud.Host and sends play/next/seek/like/DeArrow back to it; quality and
subtitles open SmartTube's own dialogs.
SmartTube's own controls stay in place: they are hidden while the HUD is shown and come back when the user
chooses "SmartTube controls: classic" in the player menu, or when the suggestions row is opened.
All anchors are checked before any file is written.
"""
import argparse
from pathlib import Path

PLAYBACK = 'smarttubetv/src/main/java/com/liskovsoft/smartyoutubetv2/tv/ui/playback'
CINEMA_FILES = ('QuestHud.java', 'QuestTheme.java', 'QuestCinemaController.java')
MANIFEST = 'smarttubetv/src/main/AndroidManifest.xml'
COMMON = 'common/src/main/java/com/liskovsoft/smartyoutubetv2/common'
SPONSOR = COMMON + '/app/models/playback/controllers/SponsorBlockController.java'
UNDO_BUS = COMMON + '/misc/QuestUndoBus.java'
UNDO_BUS_SOURCE = '''package com.liskovsoft.smartyoutubetv2.common.misc;

/**
 * SmartTube VR: hands SponsorBlock's "undo skip" to the VR HUD (tv module), which shows it as a chip in its
 * control bar. The listener returns false when the HUD is hidden; SponsorBlock then shows its own dialog.
 */
public final class QuestUndoBus {
    public interface Listener { boolean onUndoOffer(String label, long durationMs, Runnable undo); }

    private static Listener sListener;

    private QuestUndoBus() {}

    public static void setListener(Listener listener) { sListener = listener; }

    public static boolean offer(String label, long durationMs, Runnable undo) {
        Listener listener = sListener;
        return listener != null && listener.onUndoOffer(label, durationMs, undo);
    }
}
'''

UNDO_OLD = '''    private void showUndoSkip(List<SponsorSegment> segments, long undoPosMs, String category) {'''
UNDO_NEW = UNDO_OLD + '''
        if (com.liskovsoft.smartyoutubetv2.common.misc.QuestUndoBus.offer(category, UNDO_SKIP_MS, () -> {
            if (mActiveSegments != null && segments != null) {
                mActiveSegments.removeAll(segments);
            }
            if (getPlayer() != null) {
                setPositionMs(undoPosMs);
            }
        })) {
            return; // the VR HUD shows its own "Deshacer salto" chip
        }
'''
HUD = 'com.liskovsoft.smartyoutubetv2.tv.ui.playback.cinema.QuestHud'

PERMISSION_OLD = '    <uses-permission android:name="android.permission.INTERNET" />'
PERMISSION_NEW = (PERMISSION_OLD + '\n'
                  '    <!-- SmartTube VR: Wi-Fi signal shown in the HUD status pill -->\n'
                  '    <uses-permission android:name="android.permission.ACCESS_WIFI_STATE" />')

FIELD_OLD = '    private PlaybackPresenter mPlaybackPresenter;'
FIELD_NEW = FIELD_OLD + '\n    private ' + HUD + ' mQuestHud;'

CREATE_OLD = '''        ProgressBarManager.setup(getProgressBarManager(), (ViewGroup) root);

        return root;'''
CREATE_NEW = '''        ProgressBarManager.setup(getProgressBarManager(), (ViewGroup) root);

        // SmartTube VR HUD. Never allowed to break playback: on any error SmartTube's own controls stay.
        try {
            if (root instanceof android.widget.FrameLayout) {
                mQuestHud = new %(hud)s((android.widget.FrameLayout) root, new %(hud)s.Host() {
                    @Override public long positionMs() { return getPositionMs(); }
                    @Override public long durationMs() { return getDurationMs(); }
                    @Override public boolean isPlaying() { return PlaybackFragment.this.isPlaying(); }
                    @Override public void togglePlay() {
                        if (PlaybackFragment.this.isPlaying()) mPlaybackPresenter.onPauseClicked(); else mPlaybackPresenter.onPlayClicked();
                    }
                    @Override public void next() { mPlaybackPresenter.onNextClicked(); }
                    @Override public void seekTo(long positionMs) { setPositionMs(positionMs); }
                    @Override public String quality() {
                        FormatItem format = getVideoFormat();
                        String label = questResolution(format);
                        return !label.isEmpty() && format.getFrameRate() >= 49 ? label + Math.round(format.getFrameRate()) : label;
                    }
                    @Override public String videoSummary() {
                        // "4K • 16:9 • 60 FPS", as in the "Reproductor VR" panel of the mockup.
                        FormatItem format = getVideoFormat();
                        String label = questResolution(format);
                        if (label.isEmpty()) return "";
                        StringBuilder text = new StringBuilder(label);
                        if (format.getWidth() > 0 && format.getHeight() > 0) {
                            float ratio = (float) format.getWidth() / format.getHeight();
                            String aspect = Math.abs(ratio - 16 / 9f) < .05f ? "16:9" : Math.abs(ratio - 21 / 9f) < .08f ? "21:9"
                                    : Math.abs(ratio - 4 / 3f) < .05f ? "4:3" : Math.abs(ratio - 32 / 9f) < .1f ? "32:9"
                                    : String.format(java.util.Locale.US, "%%.2f:1", ratio);
                            text.append(" \\u2022 ").append(aspect);
                        }
                        if (format.getFrameRate() > 0) text.append(" \\u2022 ").append(Math.round(format.getFrameRate())).append(" FPS");
                        return text.toString();
                    }
                    @Override public String subtitleLabel() {
                        FormatItem format = getSubtitleFormat();
                        String lang = format != null && !FormatItem.SUBTITLE_NONE.equals(format) ? format.getLanguage() : null;
                        if (lang == null || lang.isEmpty()) {
                            return "es".equals(java.util.Locale.getDefault().getLanguage()) ? "Desactivados" : "Off";
                        }
                        String name = new java.util.Locale(lang.split("[-_]")[0]).getDisplayLanguage();
                        return name.isEmpty() ? lang : name.substring(0, 1).toUpperCase() + name.substring(1);
                    }
                    @Override public void openVideoSettings() {
                        mPlaybackPresenter.onButtonClicked(R.id.lb_control_high_quality, getButtonState(R.id.lb_control_high_quality));
                    }
                    @Override public void openSubtitles() {
                        mPlaybackPresenter.onButtonLongClicked(R.id.lb_control_closed_captioning, getButtonState(R.id.lb_control_closed_captioning));
                    }
                    @Override public void openScreenSettings() { showCinemaScreen(); }
                    @Override public void like() { mPlaybackPresenter.onButtonClicked(R.id.action_thumbs_up, getButtonState(R.id.action_thumbs_up)); }
                    @Override public void dislike() { mPlaybackPresenter.onButtonClicked(R.id.action_thumbs_down, getButtonState(R.id.action_thumbs_down)); }
                    @Override public void registerUndo(%(hud)s.UndoTarget target) {
                        com.liskovsoft.smartyoutubetv2.common.misc.QuestUndoBus.setListener(target == null ? null : target::offer);
                    }
                    @Override public String likes() { Video video = getVideo(); return video != null ? video.likeCount : null; }
                    @Override public String dislikes() { Video video = getVideo(); return video != null ? video.dislikeCount : null; }
                    @Override public boolean deArrowOn() {
                        return com.liskovsoft.smartyoutubetv2.common.prefs.DeArrowData.instance(getContext()).isDeArrowEnabled();
                    }
                    @Override public void toggleDeArrow() {
                        com.liskovsoft.smartyoutubetv2.common.prefs.DeArrowData data = com.liskovsoft.smartyoutubetv2.common.prefs.DeArrowData.instance(getContext());
                        boolean on = !data.isDeArrowEnabled();
                        data.setDeArrowEnabled(on); data.setReplaceTitlesEnabled(on); data.setReplaceThumbnailsEnabled(on);
                    }
                    @Override public void openOptions() { showCinemaMenu(); }
                    @Override public void toggleFullscreen() { toggleCinemaScenery(); }
                    @Override public void syncVisibility() { updateQuestHud(); }
                });
            }
        } catch (Throwable e) {
            mQuestHud = null; // fall back to SmartTube's own controls
        }

        return root;''' % {'hud': HUD}

BACKGROUND_OLD = '''        if (isOverlayShown()) {
            setBackgroundResource('''
BACKGROUND_NEW = '''        if (isOverlayShown() && !isQuestHudActive()) {
            setBackgroundResource('''

SHOW_OLD = '''        mIsControlsShownPreviously = true;
    }'''
SHOW_NEW = '''        mIsControlsShownPreviously = true;
        updateQuestHud();
    }'''
HIDE_OLD = '''        mIsControlsShownPreviously = false;
    }'''
HIDE_NEW = '''        mIsControlsShownPreviously = false;
        updateQuestHud();
    }

    /** The VR HUD replaces SmartTube's controls row while the controls (not the suggestions) are shown. */
    private boolean isQuestHudActive() {
        return mQuestHud != null && isControlsOverlayVisible() && getPlayerRowIndex() == 0 && !%(hud)s.isClassic(getContext());
    }

    /** "4K", "2K", "1080p"... of the playing video; empty when unknown. */
    private static String questResolution(FormatItem format) {
        int height = format != null ? format.getHeight() : 0;
        return height >= 4320 ? "8K" : height >= 2160 ? "4K" : height >= 1440 ? "2K" : height > 0 ? height + "p" : "";
    }

    private void updateQuestHud() {
        if (mQuestHud == null) return;
        boolean hud = isQuestHudActive();
        mQuestHud.setControlsShown(hud);
        View root = getView();
        View dock = root != null ? root.findViewById(R.id.playback_controls_dock) : null;
        if (dock != null) dock.setAlpha(hud ? 0f : 1f); // hidden but still focusable: SmartTube's key handling keeps working
    }''' % {'hud': HUD}

SEGMENTS_OLD = '''        if (mPlayerGlue != null) {
            mPlayerGlue.setSeekBarSegments(segments);
        }'''
SEGMENTS_NEW = SEGMENTS_OLD + '''
        if (mQuestHud != null) {
            java.util.List<float[]> marks = new java.util.ArrayList<>();
            if (segments != null) {
                for (SeekBarSegment segment : segments) {
                    marks.add(new float[]{segment.startProgress, segment.endProgress, segment.color});
                }
            }
            mQuestHud.setSegments(marks);
        }'''

DESTROY_OLD = '''    public void onDestroy() {
        super.onDestroy();'''
DESTROY_NEW = DESTROY_OLD + '''

        if (mQuestHud != null) {
            mQuestHud.release();
            mQuestHud = null;
        }'''

FULLSCREEN_OLD = '''    public void setCinemaPip(boolean pip) {'''
FULLSCREEN_NEW = '''    public void toggleCinemaScenery() {
        if (mQuestCinema != null) mQuestCinema.toggleScenery();
    }

    public void showCinemaScreen() {
        if (mQuestCinema != null) mQuestCinema.showScreenSettings();
    }

    public void setCinemaPip(boolean pip) {'''


def plan(root):
    play = root / PLAYBACK
    changes = {
        play / 'PlaybackFragment.java': [
            (FIELD_OLD, FIELD_NEW), (CREATE_OLD, CREATE_NEW), (BACKGROUND_OLD, BACKGROUND_NEW),
            (SHOW_OLD, SHOW_NEW), (HIDE_OLD, HIDE_NEW), (SEGMENTS_OLD, SEGMENTS_NEW), (DESTROY_OLD, DESTROY_NEW),
        ],
        play / 'mod/surface/SurfacePlaybackFragment.java': [(FULLSCREEN_OLD, FULLSCREEN_NEW)],
        root / MANIFEST: [(PERMISSION_OLD, PERMISSION_NEW)],
        root / SPONSOR: [(UNDO_OLD, UNDO_NEW)],
    }
    planned = {}
    for path, replacements in changes.items():
        content = path.read_text(encoding='utf-8')
        for old, new in replacements:
            if content.count(old) != 1:
                raise SystemExit(f'HUD patch anchor mismatch in {path}: {old[:70]!r}')
            content = content.replace(old, new, 1)
        planned[path] = content
    bus = root / UNDO_BUS
    if bus.exists():
        raise SystemExit(f'HUD patch: {bus} already exists')
    planned[bus] = UNDO_BUS_SOURCE
    return planned


def main(root):
    for name in CINEMA_FILES:
        if not (root / PLAYBACK / 'cinema' / name).exists():
            raise SystemExit(f'smarttube_cinema.py must run first: missing {name}')
    planned = plan(root)
    for path, content in planned.items():
        path.parent.mkdir(parents=True, exist_ok=True)
        path.write_text(content, encoding='utf-8')
        print('HUD PATCHED', path)


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--source', type=Path, required=True)
    main(parser.parse_args().source)
