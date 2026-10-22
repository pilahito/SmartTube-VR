#!/usr/bin/env python3
"""Connect the VR player HUD (QuestHud, docs/interfaz/reproductor.jpg) to SmartTube's playback fragment.

Must run after smarttube_cinema.py, which copies QuestHud/QuestTheme and the cinema controller.
The HUD only reads the player through QuestHud.Host and sends play/next/seek/DeArrow back to it.
SmartTube's own controls stay in place: they are hidden while the HUD is shown and come back when the user
chooses "SmartTube controls: classic" in the player menu, or when the suggestions row is opened.
All anchors are checked before any file is written.
"""
import argparse
from pathlib import Path

PLAYBACK = 'smarttubetv/src/main/java/com/liskovsoft/smartyoutubetv2/tv/ui/playback'
CINEMA_FILES = ('QuestHud.java', 'QuestTheme.java', 'QuestCinemaController.java')
MANIFEST = 'smarttubetv/src/main/AndroidManifest.xml'
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
                        if (format == null) return "";
                        int height = format.getHeight();
                        String label = height >= 4320 ? "8K" : height >= 2160 ? "4K" : height >= 1440 ? "2K" : height > 0 ? height + "p" : "";
                        return !label.isEmpty() && format.getFrameRate() >= 49 ? label + Math.round(format.getFrameRate()) : label;
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
    }
    planned = {}
    for path, replacements in changes.items():
        content = path.read_text(encoding='utf-8')
        for old, new in replacements:
            if content.count(old) != 1:
                raise SystemExit(f'HUD patch anchor mismatch in {path}: {old[:70]!r}')
            content = content.replace(old, new, 1)
        planned[path] = content
    return planned


def main(root):
    for name in CINEMA_FILES:
        if not (root / PLAYBACK / 'cinema' / name).exists():
            raise SystemExit(f'smarttube_cinema.py must run first: missing {name}')
    planned = plan(root)
    for path, content in planned.items():
        path.write_text(content, encoding='utf-8')
        print('HUD PATCHED', path)


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--source', type=Path, required=True)
    main(parser.parse_args().source)
