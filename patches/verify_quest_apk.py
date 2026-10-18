#!/usr/bin/env python3
"""Check the packaged APK, after manifest merging, before uploading it."""
import argparse
import hashlib
import json
from pathlib import Path
import re
import subprocess
import zipfile


def inspect_badging(text):
    package = re.search(r"^package: name='([^']+)'", text, re.M)
    if not package or not package[1].endswith('.vr'):
        raise ValueError('The APK must use its own SmartTube VR (.vr) package')
    required = re.findall(r"^uses-feature: name='([^']+)'", text, re.M)
    for feature in ('android.software.leanback', 'android.hardware.touchscreen'):
        if feature in required:
            raise ValueError(f'Quest-incompatible required feature: {feature}')
    launcher = re.search(r"^launchable-activity: name='([^']+)'", text, re.M)
    if not launcher:
        raise ValueError('No enabled launcher activity in packaged manifest')
    if not launcher[1].endswith('.QuestHomeActivity'):
        raise ValueError(f'The launcher must open the SmartTube VR home, not {launcher[1]}')
    if not re.search(r"^uses-permission(?:-sdk-\d+)?: name='android.permission.INTERNET'", text, re.M):
        raise ValueError('INTERNET permission is missing')
    sdk = re.search(r"^sdkVersion:'(\d+)'", text, re.M)
    if not sdk:
        raise ValueError('Missing minimum Android API version')
    return {'package': package[1], 'launcher': launcher[1], 'min_sdk': int(sdk[1]),
            'required_features': required}


def verify(apk, aapt, apksigner):
    # Both commands must succeed; a valid ZIP alone is not a valid signed APK.
    subprocess.run([str(apksigner), 'verify', '--verbose', str(apk)], check=True)
    result = subprocess.run([str(aapt), 'dump', 'badging', str(apk)],
                            check=True, capture_output=True, text=True)
    details = inspect_badging(result.stdout)
    with zipfile.ZipFile(apk) as archive:
        names = archive.namelist()
        if not any(n.startswith('lib/arm64-v8a/') and n.endswith('.so') for n in names):
            raise ValueError('The APK contains no ARM64 native libraries')
        bad = archive.testzip()
        if bad:
            raise ValueError(f'Corrupt ZIP entry: {bad}')
    with apk.open('rb') as stream:
        digest = hashlib.file_digest(stream, 'sha256').hexdigest()
    details.update(file=apk.name, bytes=apk.stat().st_size, sha256=digest)
    return details


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--apk', type=Path, required=True)
    parser.add_argument('--build-tools', type=Path, required=True)
    parser.add_argument('--report', type=Path, required=True)
    args = parser.parse_args()
    details = verify(args.apk, args.build_tools / 'aapt', args.build_tools / 'apksigner')
    args.report.parent.mkdir(parents=True, exist_ok=True)
    args.report.write_text(json.dumps(details, indent=2) + '\n', encoding='utf-8')
    args.apk.with_suffix('.apk.sha256').write_text(
        f"{details['sha256']}  {args.apk.name}\n", encoding='utf-8')
    print(json.dumps(details, indent=2))


if __name__ == '__main__':
    main()
