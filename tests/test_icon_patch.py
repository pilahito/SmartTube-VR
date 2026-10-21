import importlib.util
from pathlib import Path
import tempfile
import unittest

PATCH = Path(__file__).resolve().parents[1] / 'patches/smarttube_icon.py'
spec = importlib.util.spec_from_file_location('icon_patch', PATCH)
icon = importlib.util.module_from_spec(spec)
spec.loader.exec_module(icon)


def fake_png(width, height=10):
    """Cabecera PNG mínima: basta para leer el ancho."""
    ihdr = b'\x00\x00\x00\x0dIHDR' + width.to_bytes(4, 'big') + height.to_bytes(4, 'big') + b'\x08\x06\x00\x00\x00'
    return b'\x89PNG\r\n\x1a\n' + ihdr


class IconPatchTests(unittest.TestCase):
    def tree(self, root, banners):
        for flavor, banner_width in banners.items():
            folder = root / 'smarttubetv/src' / flavor / 'res/mipmap-nodpi'
            folder.mkdir(parents=True)
            (folder / 'app_icon.png').write_bytes(fake_png(320))
            (folder / 'app_banner.png').write_bytes(fake_png(banner_width))
        (root / 'smarttubetv/src/main/res/mipmap-nodpi/app_icon_alt.png').write_bytes(fake_png(320))

    def test_replaces_icons_and_picks_banner_by_size(self):
        with tempfile.TemporaryDirectory() as folder:
            root = Path(folder)
            self.tree(root, {'main': 320, 'stfdroid': 640})
            icon.main(root)
            new_icon = (icon.ICONS / 'app_icon.png').read_bytes()
            self.assertEqual((root / 'smarttubetv/src/stfdroid/res/mipmap-nodpi/app_icon.png').read_bytes(), new_icon)
            self.assertEqual((root / 'smarttubetv/src/main/res/mipmap-nodpi/app_icon_alt.png').read_bytes(),
                             (icon.ICONS / 'app_icon_alt.png').read_bytes())
            self.assertEqual((root / 'smarttubetv/src/stfdroid/res/mipmap-nodpi/app_banner.png').read_bytes(),
                             (icon.ICONS / 'app_banner_640.png').read_bytes())
            self.assertEqual((root / 'smarttubetv/src/main/res/mipmap-nodpi/app_banner.png').read_bytes(),
                             (icon.ICONS / 'app_banner_320.png').read_bytes())

    def test_fails_when_upstream_layout_changes(self):
        with tempfile.TemporaryDirectory() as folder:
            with self.assertRaises(SystemExit):
                icon.main(Path(folder))

    def test_shipped_images_have_the_expected_sizes(self):
        self.assertEqual(icon.png_width(icon.ICONS / 'app_icon.png'), 320)
        self.assertEqual(icon.png_width(icon.ICONS / 'app_banner_640.png'), 640)
        self.assertEqual(icon.png_width(icon.ICONS / 'app_banner_320.png'), 320)


if __name__ == '__main__':
    unittest.main()
