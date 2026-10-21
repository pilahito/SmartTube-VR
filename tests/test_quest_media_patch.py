import importlib.util
from pathlib import Path
import tempfile
import unittest

PATCH = Path(__file__).resolve().parents[1] / 'patches/smarttube_quest_media.py'
spec = importlib.util.spec_from_file_location('quest_media_patch', PATCH)
qm = importlib.util.module_from_spec(spec)
spec.loader.exec_module(qm)


def fixture(root, crlf=True):
    """Minimal upstream-shaped files containing exactly the anchors."""
    files = {}
    for rel, changes in qm.CHANGES.items():
        body = ''.join('// before\n' + old + '\n' for old, _ in changes)
        if rel in (qm.STRINGS, qm.STRINGS_ES):
            body = '<resources>\n    <string name="x">x</string>\n</resources>\n'
        path = root / rel
        path.parent.mkdir(parents=True, exist_ok=True)
        path.write_bytes((body.replace('\n', '\r\n') if crlf else body).encode())
        files[rel] = path
    return files


class QuestMediaPatchTests(unittest.TestCase):
    def test_applies_all_features_and_keeps_crlf(self):
        with tempfile.TemporaryDirectory() as d:
            files = fixture(Path(d))
            qm.main(d)
            ctrl = files[qm.CONTROLLER].read_bytes().decode()
            self.assertIn('showUndoSkip(foundSegments, undoPosMs, skipMessage)', ctrl)
            self.assertIn('UNDO_SKIP_MS = 5000', ctrl)
            self.assertIn('mActiveSegments.removeAll(segments)', ctrl)
            self.assertNotIn('\n', ctrl.replace('\r\n', ''))
            sb = files[qm.SB_DATA].read_text()
            self.assertIn('CATEGORY_SPONSOR, R.color.yellow', sb)
            self.assertIn('CATEGORY_SELF_PROMO, R.color.green', sb)
            self.assertEqual(files[qm.DEARROW].read_text().count(', true)'), 3)
            player = files[qm.PLAYER].read_text()
            for model in ('Quest 3', 'Quest 3S', 'Quest Pro'):
                self.assertIn(f'put("{model}", FormatItem.VIDEO_4K_VP9_60)', player)
            self.assertIn('parseFloat(split, 39, 1.35f)', player)
            self.assertIn('Deshacer: %s', files[qm.STRINGS_ES].read_text())

    def test_undo_is_never_offered_in_background(self):
        new = dict(qm.CHANGES[qm.CONTROLLER])
        text = ''.join(new.values())
        self.assertIn('if (willSkip && !isBackground)', text)

    def test_anchor_mismatch_writes_nothing(self):
        with tempfile.TemporaryDirectory() as d:
            files = fixture(Path(d))
            before = {p: p.read_bytes() for p in files.values()}
            files[qm.PLAYER].write_text('upstream changed')
            before[files[qm.PLAYER]] = files[qm.PLAYER].read_bytes()
            with self.assertRaises(SystemExit):
                qm.main(d)
            for p, b in before.items():
                self.assertEqual(p.read_bytes(), b)

    def test_second_apply_is_rejected_without_writing(self):
        with tempfile.TemporaryDirectory() as d:
            files = fixture(Path(d), crlf=False)
            qm.main(d)
            before = {p: p.read_bytes() for p in files.values()}
            with self.assertRaises(SystemExit):
                qm.main(d)
            for p, b in before.items():
                self.assertEqual(p.read_bytes(), b)

    def test_missing_file_fails(self):
        with tempfile.TemporaryDirectory() as d:
            with self.assertRaises(SystemExit):
                qm.main(d)


@unittest.skipUnless(Path('/workspace/smarttube-upstream/.git').exists(), 'upstream checkout not present')
class RealUpstreamTest(unittest.TestCase):
    def test_plan_matches_pinned_upstream(self):
        plan = qm.plan(Path('/workspace/smarttube-upstream'))
        self.assertEqual(len(plan), len(qm.CHANGES))


if __name__ == '__main__':
    unittest.main()
