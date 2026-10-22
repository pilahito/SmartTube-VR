import importlib.util
from pathlib import Path
import tempfile
import unittest

PATCH = Path(__file__).resolve().parents[1] / 'patches/smarttube_hud.py'
spec = importlib.util.spec_from_file_location('hud_patch', PATCH)
hud = importlib.util.module_from_spec(spec)
spec.loader.exec_module(hud)


class HudPatchTests(unittest.TestCase):
    def fixture(self, root, with_cinema=True):
        play = root / hud.PLAYBACK
        fragment = play / 'PlaybackFragment.java'
        fragment.parent.mkdir(parents=True)
        fragment.write_text('\n'.join([
            hud.FIELD_OLD, hud.CREATE_OLD, hud.BACKGROUND_OLD, hud.SHOW_OLD, hud.HIDE_OLD,
            hud.SEGMENTS_OLD, hud.DESTROY_OLD, '']))
        surface = play / 'mod/surface/SurfacePlaybackFragment.java'
        surface.parent.mkdir(parents=True)
        surface.write_text(hud.FULLSCREEN_OLD + '\n')
        manifest = root / hud.MANIFEST
        manifest.parent.mkdir(parents=True)
        manifest.write_text(hud.PERMISSION_OLD + '\n')
        if with_cinema:
            (play / 'cinema').mkdir()
            for name in hud.CINEMA_FILES:
                (play / 'cinema' / name).write_text('class X {}')
        return fragment, surface, manifest

    def test_connects_hud_to_the_fragment(self):
        with tempfile.TemporaryDirectory() as folder:
            root = Path(folder)
            fragment, surface, manifest = self.fixture(root)
            hud.main(root)
            text = fragment.read_text()
            self.assertIn('new com.liskovsoft.smartyoutubetv2.tv.ui.playback.cinema.QuestHud(', text)
            self.assertIn('catch (Throwable e)', text)  # the HUD can never break playback
            self.assertIn('updateQuestHud();', text)
            self.assertIn('mQuestHud.setSegments(marks)', text)
            self.assertIn('mQuestHud.release()', text)
            self.assertIn('toggleCinemaScenery', surface.read_text())
            self.assertIn('ACCESS_WIFI_STATE', manifest.read_text())

    def test_needs_the_cinema_patch_first(self):
        with tempfile.TemporaryDirectory() as folder:
            root = Path(folder)
            fragment, surface, manifest = self.fixture(root, with_cinema=False)
            before = fragment.read_bytes()
            with self.assertRaises(SystemExit):
                hud.main(root)
            self.assertEqual(fragment.read_bytes(), before)

    def test_anchor_failure_does_not_leave_half_patched_checkout(self):
        with tempfile.TemporaryDirectory() as folder:
            root = Path(folder)
            fragment, surface, manifest = self.fixture(root)
            manifest.write_text('Changed upstream manifest')
            before = fragment.read_bytes()
            with self.assertRaises(SystemExit):
                hud.main(root)
            self.assertEqual(fragment.read_bytes(), before)
            self.assertEqual(surface.read_text(), hud.FULLSCREEN_OLD + '\n')

    def test_second_apply_is_rejected_without_writing(self):
        with tempfile.TemporaryDirectory() as folder:
            root = Path(folder)
            fragment, surface, manifest = self.fixture(root)
            hud.main(root)
            before = fragment.read_bytes()
            with self.assertRaises(SystemExit):
                hud.main(root)
            self.assertEqual(fragment.read_bytes(), before)
