import importlib.util
from pathlib import Path
import tempfile
import unittest

PATCH = Path(__file__).resolve().parents[1] / 'patches/smarttube_home.py'
spec = importlib.util.spec_from_file_location('home_patch', PATCH)
home = importlib.util.module_from_spec(spec)
spec.loader.exec_module(home)


class HomePatchTests(unittest.TestCase):
    def fixture(self, root, with_cinema=True):
        manifest = root / home.MANIFEST
        manifest.parent.mkdir(parents=True)
        manifest.write_text(home.QUERIES_OLD + '\n<application>\n' + home.SPLASH_OLD
                            + '\n' + home.ALIAS_OLD + '\n</application>\n')
        ui = root / home.JAVA
        (ui / 'main').mkdir(parents=True)
        (ui / 'main/SplashActivity.java').write_text('class SplashActivity {}')
        if with_cinema:
            (ui / 'playback/cinema').mkdir(parents=True)
            for name in home.CINEMA_DEPENDENCIES:
                (ui / 'playback/cinema' / name).write_text('class X {}')
        return manifest

    def test_moves_launcher_to_home_and_adds_queries(self):
        with tempfile.TemporaryDirectory() as folder:
            root = Path(folder)
            manifest = self.fixture(root)
            home.main(root)
            text = manifest.read_text()
            self.assertIn('ui.home.QuestHomeActivity', text)
            self.assertEqual(text.count('android.intent.category.LAUNCHER"'), 2)  # query + home
            self.assertIn('LEANBACK_LAUNCHER', text)
            self.assertIn('<package android:name="com.brave.browser" />', text)
            self.assertEqual(sorted(p.name for p in (root / home.JAVA / 'home').glob('*.java')),
                             sorted(home.SOURCES))

    def test_requires_cinema_sources_without_writing(self):
        with tempfile.TemporaryDirectory() as folder:
            root = Path(folder)
            manifest = self.fixture(root, with_cinema=False)
            before = manifest.read_bytes()
            with self.assertRaises(SystemExit):
                home.main(root)
            self.assertEqual(manifest.read_bytes(), before)
            self.assertFalse((root / home.JAVA / 'home').exists())

    def test_anchor_mismatch_and_second_apply_are_rejected(self):
        with tempfile.TemporaryDirectory() as folder:
            root = Path(folder)
            manifest = self.fixture(root)
            manifest.write_text(manifest.read_text().replace(home.ALIAS_OLD, '<!-- changed -->'))
            with self.assertRaises(SystemExit):
                home.main(root)
            self.assertFalse((root / home.JAVA / 'home').exists())
        with tempfile.TemporaryDirectory() as folder:
            root = Path(folder)
            manifest = self.fixture(root)
            home.main(root)
            before = manifest.read_bytes()
            with self.assertRaises(SystemExit):
                home.main(root)
            self.assertEqual(manifest.read_bytes(), before)


if __name__ == '__main__':
    unittest.main()
