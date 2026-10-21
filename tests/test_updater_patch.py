import json
import tempfile
import unittest
from pathlib import Path

from patches import smarttube_updater as up

GRADLE = '''android {
    defaultConfig {
        applicationId "app.smarttube"
        versionCode 2458
        versionName "32.68"
    }
}
'''
UPSTREAM = '''<resources><string-array name="update_urls">
<item>https://github.com/yuliskov/SmartTubeNext/releases/download/latest/smarttube_stable2.json</item>
</string-array></resources>'''


class UpdaterPatchTest(unittest.TestCase):
    def _tree(self, root):
        for flavor in ("main", "ststable", "stbeta"):
            x = root / "common/src" / flavor / "res/values/update_urls.xml"
            x.parent.mkdir(parents=True)
            x.write_text(UPSTREAM)
        g = root / "smarttubetv/build.gradle"
        g.parent.mkdir(parents=True)
        g.write_text(GRADLE)

    def test_rewrites_all_update_urls_to_our_repo(self):
        with tempfile.TemporaryDirectory() as t:
            root = Path(t)
            self._tree(root)
            up.apply(root, 7)
            xmls = list((root / "common/src").glob("*/res/values/update_urls.xml"))
            self.assertEqual(len(xmls), 3)
            for x in xmls:
                s = x.read_text()
                self.assertNotIn("yuliskov", s)
                self.assertIn("https://github.com/pilahito/SmartTube-VR/releases/latest/download/smarttube_vr.json", s)

    def test_version_code_grows_and_beats_upstream(self):
        with tempfile.TemporaryDirectory() as t:
            root = Path(t)
            self._tree(root)
            up.apply(root, 42)
            g = (root / "smarttubetv/build.gradle").read_text()
            self.assertIn("versionCode 100042", g)
            self.assertIn('versionName "32.68-vr.42"', g)
            self.assertNotIn("2458", g)
        self.assertGreater(up.version_code(1), 2458)
        self.assertGreater(up.version_code(2), up.version_code(1))
        with self.assertRaises(ValueError):
            up.version_code(0)

    def test_missing_layout_fails_loudly(self):
        with tempfile.TemporaryDirectory() as t:
            with self.assertRaises(SystemExit):
                up.apply(Path(t), 1)

    def test_manifest_matches_appupdatechecker2_format(self):
        with tempfile.TemporaryDirectory() as t:
            out = Path(t) / "m.json"
            up.main(["--run-number", "5", "--write-manifest", str(out),
                     "--version-name", "32.68-vr.5", "--tag", "vr-5", "--changelog", "Hola"])
            m = json.loads(out.read_text())
        pkg = m["package"]
        self.assertEqual(pkg["downloadUrlList_arm64-v8a"],
                         ["https://github.com/pilahito/SmartTube-VR/releases/download/vr-5/SmartTubeVR_arm64-v8a.apk"])
        self.assertTrue(pkg["downloadUrl"].endswith("/vr-5/SmartTubeVR_universal.apk"))
        self.assertEqual(m["32.68-vr.5"], {"versionCode": 100005, "changelog": ["Hola"]})
        # Every non-"package" key must carry an int versionCode (AppVersionChecker.getInt).
        for k, v in m.items():
            if k != "package":
                self.assertIsInstance(v["versionCode"], int)


if __name__ == "__main__":
    unittest.main()
