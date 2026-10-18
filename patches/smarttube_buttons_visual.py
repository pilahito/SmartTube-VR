#!/usr/bin/env python3
"""Visual-only button patch for the original SmartTube checkout.
No Java, control handlers, player, SponsorBlock, or DeArrow logic changed.
"""
from pathlib import Path
import argparse

def replace_once(path, old, new):
    source = path.read_text(encoding="utf-8")
    if source.count(old) != 1:
        raise SystemExit("Unexpected upstream resource; refusing to patch: " + str(path))
    path.write_text(source.replace(old, new, 1), encoding="utf-8")
    print("UI resource updated:", path)

def main(root):
    ui = root / "smarttubetv/src/main/res"
    colors = ui / "values/colors.xml"
    replace_once(colors,
                 '<color name="lb_control_button_color">#86EEEEEE</color>',
                 '<color name="lb_control_button_color">#B4FFFFFF</color>')
    # More legible, understated outline for television/Quest interface cards.
    card = ui / "drawable-v21/lb_card_outline.xml"
    replace_once(card, 'android:color="#4dffffff"',
                 'android:color="#73FFFFFF"')
    # Does not change the clickable surface, focus handling or dimensions.
    button = ui / "drawable-v21/lb_control_button_secondary.xml"
    replace_once(button,
                 '<stroke\n                        android:width="1dp"\n                        android:color="#00FFFFFF"/>',
                 '<stroke\n                        android:width="1dp"\n                        android:color="#33FFFFFF"/>')
    # Subtle contrast improvements to search/account orb controls.
    replace_once(colors, '<color name="orb_color">@color/grey</color>',
                 '<color name="orb_color">#374151</color>')
    replace_once(colors, '<color name="orb_bright_color">@color/grey</color>',
                 '<color name="orb_bright_color">#536B8A</color>')
    # Improve readability of progress time without moving controls.
    dims = ui / "values/dimens.xml"
    replace_once(dims, '<dimen name="lb_playback_controls_time_text_size">16sp</dimen>',
                 '<dimen name="lb_playback_controls_time_text_size">18sp</dimen>')
    print("Only Android colors, dimensions and drawable XML changed. No Java logic or input handlers modified.")

if __name__ == "__main__":
    ap = argparse.ArgumentParser()
    ap.add_argument("--source", type=Path, required=True)
    main(ap.parse_args().source)
