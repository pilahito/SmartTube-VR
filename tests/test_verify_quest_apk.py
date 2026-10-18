import hashlib
from pathlib import Path
import subprocess
import tempfile
import unittest
from unittest.mock import patch
import zipfile

from patches.verify_quest_apk import inspect_badging, verify


BADGING = """package: name='com.example.smarttube.vr' versionCode='1'
sdkVersion:'21'
uses-permission: name='android.permission.INTERNET'
uses-feature-not-required: name='android.software.leanback'
uses-feature-not-required: name='android.hardware.touchscreen'
launchable-activity: name='com.example.home.QuestHomeActivity' label='SmartTube VR' icon=''
"""


class QuestApkTests(unittest.TestCase):
    def test_optional_features_are_accepted(self):
        self.assertEqual(inspect_badging(BADGING)['required_features'], [])

    def test_merged_required_features_are_rejected(self):
        for feature in ('android.software.leanback', 'android.hardware.touchscreen'):
            with self.subTest(feature=feature), self.assertRaises(ValueError):
                inspect_badging(BADGING + f"uses-feature: name='{feature}'\n")

    def test_official_package_is_rejected(self):
        with self.assertRaises(ValueError):
            inspect_badging(BADGING.replace('.vr', ''))

    def test_missing_launcher_permission_or_sdk_is_rejected(self):
        for prefix in ('launchable-activity:', 'uses-permission:', 'sdkVersion:'):
            with self.subTest(prefix=prefix), self.assertRaises(ValueError):
                inspect_badging('\n'.join(line for line in BADGING.splitlines()
                                          if not line.startswith(prefix)))

    def test_launcher_must_be_vr_home(self):
        self.assertTrue(inspect_badging(BADGING)['launcher'].endswith('.QuestHomeActivity'))
        with self.assertRaises(ValueError):
            inspect_badging(BADGING.replace('home.QuestHomeActivity', 'main.SplashActivity'))

    def make_apk(self, root, arm64=True):
        apk = Path(root) / 'test.apk'
        with zipfile.ZipFile(apk, 'w') as archive:
            archive.writestr('AndroidManifest.xml', b'manifest fixture')
            archive.writestr('lib/' + ('arm64-v8a' if arm64 else 'armeabi-v7a') + '/test.so', b'native fixture')
        return apk

    @patch('patches.verify_quest_apk.subprocess.run')
    def test_reports_real_checksum_and_runs_signature_check(self, run):
        run.return_value = subprocess.CompletedProcess([], 0, stdout=BADGING)
        with tempfile.TemporaryDirectory() as root:
            apk = self.make_apk(root)
            result = verify(apk, Path('/sdk/aapt'), Path('/sdk/apksigner'))
            self.assertEqual(result['sha256'], hashlib.sha256(apk.read_bytes()).hexdigest())
            self.assertEqual(run.call_args_list[0].args[0][1], 'verify')
            self.assertTrue(run.call_args_list[0].kwargs['check'])

    @patch('patches.verify_quest_apk.subprocess.run')
    def test_arm32_only_apk_is_rejected(self, run):
        run.return_value = subprocess.CompletedProcess([], 0, stdout=BADGING)
        with tempfile.TemporaryDirectory() as root, self.assertRaises(ValueError):
            verify(self.make_apk(root, arm64=False), Path('aapt'), Path('apksigner'))

    @patch('patches.verify_quest_apk.subprocess.run', side_effect=subprocess.CalledProcessError(1, 'apksigner'))
    def test_invalid_signature_stops_validation(self, run):
        with self.assertRaises(subprocess.CalledProcessError):
            verify(Path('test.apk'), Path('aapt'), Path('apksigner'))
        self.assertEqual(run.call_count, 1)


if __name__ == '__main__':
    unittest.main()
