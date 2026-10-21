#!/usr/bin/env python3
"""Point SmartTube's built-in updater at pilahito/SmartTube-VR releases.

Upstream (yuliskov/SmartTube ffa16426) reads R.array.update_urls (common module)
and feeds it to appupdatechecker2, which downloads a JSON manifest, compares the
highest "versionCode" with the installed one, downloads
package.downloadUrlList_<primary ABI> and installs it (REQUEST_INSTALL_PACKAGES
is already declared by appupdatechecker2). The stfdroid flavor we build ships an
empty array (no updates) and stable/beta point at yuliskov's manifests, which
would replace our build with upstream. Every update_urls.xml is rewritten to our
own manifest, and smarttubetv/build.gradle gets a versionCode/versionName that
grows with each CI build so Android accepts it as an update.

The manifest itself is produced in CI by `--write-manifest` and attached to the
GitHub Release; /releases/latest/download/<asset> always resolves to the newest
non-prerelease release.
"""
import argparse
import json
import re
import sys
from pathlib import Path

REPO = "pilahito/SmartTube-VR"
MANIFEST_NAME = "smarttube_vr.json"
ARM64_APK = "SmartTubeVR_arm64-v8a.apk"
UNIVERSAL_APK = "SmartTubeVR_universal.apk"
LATEST = f"https://github.com/{REPO}/releases/latest/download"
MANIFEST_URL = f"{LATEST}/{MANIFEST_NAME}"
# Upstream defaultConfig versionCode is 2458; stay well above it.
VERSION_CODE_BASE = 100000

UPDATE_URLS_XML = f"""<?xml version="1.0" encoding="utf-8"?>
<!-- SmartTube VR: actualizaciones desde las GitHub Releases de {REPO} -->
<resources>
    <string-array name="update_urls">
        <item>{MANIFEST_URL}</item>
    </string-array>
</resources>
"""


def version_code(run_number: int) -> int:
    if run_number < 1:
        raise ValueError("run number must be >= 1")
    return VERSION_CODE_BASE + run_number


def apply(source: Path, run_number: int, base_name: str = None) -> list:
    changed = []
    common = source / "common" / "src"
    main_xml = common / "main" / "res" / "values" / "update_urls.xml"
    if not main_xml.is_file():
        raise SystemExit("common/src/main/res/values/update_urls.xml not found; upstream layout changed")
    for xml in sorted(common.glob("*/res/values/update_urls.xml")):
        xml.write_text(UPDATE_URLS_XML, encoding="utf-8")
        changed.append(f"rewrote {xml.relative_to(source)}")
    gradle = source / "smarttubetv" / "build.gradle"
    text = gradle.read_text(encoding="utf-8")
    m = re.search(r'^(\s*)versionName "([^"]+)"', text, re.M)
    if not m or not re.search(r"^\s*versionCode \d+", text, re.M):
        raise SystemExit("versionCode/versionName not found in smarttubetv/build.gradle")
    name = f"{base_name or m.group(2)}-vr.{run_number}"
    code = version_code(run_number)
    text = re.sub(r"^(\s*)versionCode \d+", rf"\g<1>versionCode {code}", text, count=1, flags=re.M)
    text = re.sub(r'^(\s*)versionName "[^"]+"', rf'\g<1>versionName "{name}"', text, count=1, flags=re.M)
    gradle.write_text(text, encoding="utf-8")
    changed.append(f"smarttubetv/build.gradle versionCode={code} versionName={name}")
    return changed


def manifest(code: int, name: str, tag: str, changelog: list) -> dict:
    base = f"https://github.com/{REPO}/releases/download/{tag}"
    arm64 = f"{base}/{ARM64_APK}"
    universal = f"{base}/{UNIVERSAL_APK}"
    return {
        "package": {
            "downloadUrl": universal,
            "downloadUrlList": [universal],
            "downloadUrlList_arm64-v8a": [arm64],
        },
        name: {"versionCode": code, "changelog": changelog or [f"SmartTube VR {name}"]},
    }


def main(argv=None):
    p = argparse.ArgumentParser()
    p.add_argument("--source", type=Path)
    p.add_argument("--run-number", type=int, required=True)
    p.add_argument("--write-manifest", type=Path)
    p.add_argument("--version-name")
    p.add_argument("--tag")
    p.add_argument("--changelog", action="append", default=[])
    a = p.parse_args(argv)
    if a.write_manifest:
        if not (a.version_name and a.tag):
            p.error("--write-manifest needs --version-name and --tag")
        data = manifest(version_code(a.run_number), a.version_name, a.tag, a.changelog)
        a.write_manifest.write_text(json.dumps(data, ensure_ascii=False, indent=2), encoding="utf-8")
        print(f"wrote {a.write_manifest}")
        return 0
    if not a.source:
        p.error("--source is required")
    for line in apply(a.source, a.run_number):
        print(line)
    return 0


if __name__ == "__main__":
    sys.exit(main())
