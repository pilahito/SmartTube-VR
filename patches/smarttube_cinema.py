#!/usr/bin/env python3
"""Integrate scenery into the genuine SmartTube playback fragment.

The Android window remains monoscopic. No OpenXR/stereo claims or replacement player.
All upstream anchors are checked before writing any files.
"""
import argparse
from pathlib import Path

PACKAGE = 'smarttubetv/src/main/java/com/liskovsoft/smartyoutubetv2/tv/ui/playback'


def main(root):
    changes = {
        root / PACKAGE / 'mod/surface/SurfacePlaybackFragment.java': [
            ('    private SurfaceWrapper mVideoSurfaceWrapper;',
             '    private com.liskovsoft.smartyoutubetv2.tv.ui.playback.cinema.QuestCinemaController mQuestCinema;\n'
             '    private SurfaceWrapper mVideoSurfaceWrapper;'),
            ('        setBackgroundType(PlaybackSupportFragment.BG_LIGHT);\n        return root;',
             '        setBackgroundType(PlaybackSupportFragment.BG_LIGHT);\n'
             '        if (root instanceof FrameLayout) {\n'
             '            mQuestCinema = new com.liskovsoft.smartyoutubetv2.tv.ui.playback.cinema.QuestCinemaController(\n'
             '                    (FrameLayout) root, mVideoSurfaceRoot, mLeanbackSubtitles);\n'
             '        }\n        return root;'),
            ('    public void onDestroyView() {\n        mVideoSurfaceWrapper = null;',
             '    public void onDestroyView() {\n'
             '        if (mQuestCinema != null) { mQuestCinema.release(); mQuestCinema = null; }\n'
             '        mVideoSurfaceWrapper = null;'),
            ('    protected void setPixelRatio(float pixelRatio) {',
             '    public void showCinemaMenu() {\n'
             '        if (mQuestCinema != null) mQuestCinema.showMenu();\n    }\n\n'
             '    public void setCinemaPip(boolean pip) {\n'
             '        if (mQuestCinema != null) mQuestCinema.setPip(pip);\n    }\n\n'
             '    protected void setPixelRatio(float pixelRatio) {')
        ],
        root / PACKAGE / 'PlaybackActivity.java': [
            ('    public boolean dispatchKeyEvent(KeyEvent event) {',
             '    public boolean dispatchKeyEvent(KeyEvent event) {\n'
             '        if (mPlaybackFragment != null && (event.getKeyCode() == KeyEvent.KEYCODE_MENU\n'
             '                || event.getKeyCode() == KeyEvent.KEYCODE_BUTTON_Y)) {\n'
             '            if (event.getAction() == KeyEvent.ACTION_DOWN && event.getRepeatCount() == 0) {\n'
             '                mPlaybackFragment.showCinemaMenu();\n'
             '            }\n            return true;\n        }'),
            ('        mPlaybackFragment.onPIPChanged(isInPictureInPictureMode);',
             '        mPlaybackFragment.onPIPChanged(isInPictureInPictureMode);\n'
             '        mPlaybackFragment.setCinemaPip(isInPictureInPictureMode);')
        ]
    }
    planned = {}
    for path, replacements in changes.items():
        content = path.read_text(encoding='utf-8')
        for old, new in replacements:
            if content.count(old) != 1:
                raise SystemExit(f'Cinema patch anchor mismatch in {path}: {old[:70]!r}')
            content = content.replace(old, new, 1)
        planned[path] = content
    for source in sorted((Path(__file__).parent / 'cinema').glob('*.java')):
        target = root / PACKAGE / 'cinema' / source.name
        if target.exists():
            raise SystemExit(f'Refusing to overwrite existing cinema source: {target}')
        planned[target] = source.read_text(encoding='utf-8')
    if len(planned) != 5:
        raise SystemExit('Expected all three cinema Java sources')
    for path, content in planned.items():
        path.parent.mkdir(parents=True, exist_ok=True)
        path.write_text(content, encoding='utf-8')
        print('CINEMA PATCHED', path)


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--source', type=Path, required=True)
    main(parser.parse_args().source)
