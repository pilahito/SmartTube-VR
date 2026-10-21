#!/usr/bin/env python3
"""Replace the original SmartTube launcher icons with the SmartTube VR ones.

The upstream icons are single PNGs in smarttubetv/src/<flavor>/res/mipmap-nodpi*:
app_icon.png, app_icon_alt.png (main only) and app_banner.png. The banner comes in
two sizes upstream (640x360 for the F-Droid flavor, 320x180 for main), so the matching
size is chosen from each file. Everything is checked before any file is written.
Regenerate the images with tools/make_icon.py.
"""
import argparse
import struct
from pathlib import Path

ICONS = Path(__file__).parent / 'icon'
RES_GLOB = 'smarttubetv/src/*/res/mipmap-nodpi*'


def png_width(path):
    """Width from the PNG IHDR chunk (no image library needed)."""
    head = path.read_bytes()[:24]
    if head[:8] != b'\x89PNG\r\n\x1a\n':
        raise SystemExit(f'Icon patch: not a PNG: {path}')
    return struct.unpack('>I', head[16:20])[0]


def plan(root):
    jobs = []
    for folder in sorted(root.glob(RES_GLOB)):
        for name in ('app_icon.png', 'app_icon_alt.png'):
            target = folder / name
            if target.is_file():
                jobs.append((target, ICONS / name))
        banner = folder / 'app_banner.png'
        if banner.is_file():
            size = 'app_banner_640.png' if png_width(banner) >= 640 else 'app_banner_320.png'
            jobs.append((banner, ICONS / size))
    return jobs


def main(root):
    jobs = plan(root)
    if not any(t.name == 'app_icon.png' for t, _ in jobs):
        raise SystemExit(f'Icon patch: no app_icon.png found under {root / RES_GLOB}')
    if not any(t.name == 'app_banner.png' for t, _ in jobs):
        raise SystemExit(f'Icon patch: no app_banner.png found under {root / RES_GLOB}')
    for _, source in jobs:
        if not source.is_file():
            raise SystemExit(f'Icon patch: missing source image {source} (run tools/make_icon.py)')
    for target, source in jobs:
        target.write_bytes(source.read_bytes())
        print('ICON PATCHED', target)


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--source', type=Path, required=True)
    main(parser.parse_args().source)
