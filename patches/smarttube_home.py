#!/usr/bin/env python3
"""Add the SmartTube VR launcher-style home (QuestHomeActivity) to the original SmartTube.

Must run after smarttube_cinema.py: the home is drawn over CinemaSceneView.
The original SplashActivity, browse UI and deep links stay intact; only the
CATEGORY_LAUNCHER entry moves to the home (LEANBACK_LAUNCHER still opens SmartTube).
All anchors are checked before any file is written.
"""
import argparse
from pathlib import Path

JAVA = 'smarttubetv/src/main/java/com/liskovsoft/smartyoutubetv2/tv/ui'
MANIFEST = 'smarttubetv/src/main/AndroidManifest.xml'
SOURCES = ('HomeTiles.java', 'QuestHomeActivity.java')
CINEMA_DEPENDENCIES = ('CinemaGeometry.java', 'CinemaSceneView.java')

QUERIES_OLD = '''    <queries>
        <intent>
            <action android:name="android.speech.RecognitionService" />
        </intent>
    </queries>'''
QUERIES_NEW = '''    <queries>
        <intent>
            <action android:name="android.speech.RecognitionService" />
        </intent>
        <!-- SmartTube VR home: list launchable apps and open web shortcuts (Android 11+ visibility) -->
        <intent>
            <action android:name="android.intent.action.MAIN" />
            <category android:name="android.intent.category.LAUNCHER" />
        </intent>
        <intent>
            <action android:name="android.intent.action.VIEW" />
            <category android:name="android.intent.category.BROWSABLE" />
            <data android:scheme="https" />
        </intent>
        <package android:name="com.brave.browser" />
    </queries>'''

SPLASH_OLD = '''                <category android:name="android.intent.category.DEFAULT"/>
                <category android:name="android.intent.category.LAUNCHER" />
                <category android:name="android.intent.category.LEANBACK_LAUNCHER" />
            </intent-filter>
            <!-- Process intents from other apps -->'''
SPLASH_NEW = '''                <category android:name="android.intent.category.DEFAULT"/>
                <!-- SmartTube VR: CATEGORY_LAUNCHER moved to QuestHomeActivity -->
                <category android:name="android.intent.category.LEANBACK_LAUNCHER" />
            </intent-filter>
            <!-- Process intents from other apps -->'''

ALIAS_OLD = '        <!-- Alt app launcher icon -->'
ALIAS_NEW = '''        <!-- SmartTube VR launcher-style home over the cinema scenery (controller/D-pad friendly) -->
        <activity
            android:name="com.liskovsoft.smartyoutubetv2.tv.ui.home.QuestHomeActivity"
            android:exported="true"
            android:label="@string/app_name"
            android:launchMode="singleTask"
            android:configChanges="screenSize|smallestScreenSize|screenLayout|orientation|layoutDirection|keyboard|keyboardHidden|navigation"
            android:screenOrientation="unspecified"
            android:theme="@android:style/Theme.Material.NoActionBar.Fullscreen">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>

        <!-- Alt app launcher icon -->'''


def main(root):
    planned = {}
    manifest = root / MANIFEST
    content = manifest.read_text(encoding='utf-8')
    if 'QuestHomeActivity' in content:
        raise SystemExit(f'Home patch already applied: {manifest}')
    for old, new in ((QUERIES_OLD, QUERIES_NEW), (SPLASH_OLD, SPLASH_NEW), (ALIAS_OLD, ALIAS_NEW)):
        if content.count(old) != 1:
            raise SystemExit(f'Home patch anchor mismatch in {manifest}: {old[:70]!r}')
        content = content.replace(old, new, 1)
    planned[manifest] = content
    for name in CINEMA_DEPENDENCIES:
        if not (root / JAVA / 'playback/cinema' / name).exists():
            raise SystemExit(f'Run smarttube_cinema.py first: missing {name}')
    if not (root / JAVA / 'main/SplashActivity.java').exists():
        raise SystemExit('Upstream SplashActivity not found')
    for name in SOURCES:
        source = Path(__file__).parent / 'home' / name
        target = root / JAVA / 'home' / name
        if target.exists():
            raise SystemExit(f'Refusing to overwrite existing home source: {target}')
        planned[target] = source.read_text(encoding='utf-8')
    for path, text in planned.items():
        path.parent.mkdir(parents=True, exist_ok=True)
        path.write_text(text, encoding='utf-8')
        print('HOME PATCHED', path)


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--source', type=Path, required=True)
    main(parser.parse_args().source)
