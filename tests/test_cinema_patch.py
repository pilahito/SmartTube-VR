import importlib.util
from pathlib import Path
import tempfile
import unittest

PATCH = Path(__file__).resolve().parents[1] / 'patches/smarttube_cinema.py'
spec = importlib.util.spec_from_file_location('cinema_patch', PATCH)
cinema = importlib.util.module_from_spec(spec)
spec.loader.exec_module(cinema)


class CinemaPatchTests(unittest.TestCase):
    def fixture(self, root):
        base = root / cinema.PACKAGE
        surface = base / 'mod/surface/SurfacePlaybackFragment.java'
        surface.parent.mkdir(parents=True)
        surface.write_text('''    private SurfaceWrapper mVideoSurfaceWrapper;
        setBackgroundType(PlaybackSupportFragment.BG_LIGHT);
        return root;
    public void onDestroyView() {
        mVideoSurfaceWrapper = null;
    protected void setPixelRatio(float pixelRatio) {
''')
        activity = base / 'PlaybackActivity.java'
        activity.write_text('''    public boolean dispatchKeyEvent(KeyEvent event) {
        mPlaybackFragment.onPIPChanged(isInPictureInPictureMode);
''')
        return surface, activity

    def test_integrates_into_original_fragment_and_pip(self):
        with tempfile.TemporaryDirectory() as folder:
            root = Path(folder)
            surface, activity = self.fixture(root)
            cinema.main(root)
            self.assertIn('mQuestCinema.release()', surface.read_text())
            self.assertIn('setCinemaPip(isInPictureInPictureMode)', activity.read_text())
            self.assertEqual(len(list((root / cinema.PACKAGE / 'cinema').glob('*.java'))), 4)
            text = surface.read_text()
            self.assertIn('setResize(resize); setAspect(aspect);', text)

    def test_anchor_failure_does_not_leave_half_patched_checkout(self):
        with tempfile.TemporaryDirectory() as folder:
            root = Path(folder)
            surface, activity = self.fixture(root)
            before = surface.read_bytes()
            activity.write_text('Changed upstream API')
            with self.assertRaises(SystemExit):
                cinema.main(root)
            self.assertEqual(surface.read_bytes(), before)
            self.assertFalse((root / cinema.PACKAGE / 'cinema').exists())

    def test_second_apply_is_rejected_without_writing(self):
        with tempfile.TemporaryDirectory() as folder:
            root = Path(folder)
            surface, activity = self.fixture(root)
            cinema.main(root)
            before = surface.read_bytes()
            with self.assertRaises(SystemExit):
                cinema.main(root)
            self.assertEqual(surface.read_bytes(), before)
