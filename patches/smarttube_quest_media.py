#!/usr/bin/env python3
"""Quest 3 media tweaks over upstream yuliskov/SmartTube (ffa16426).

Only adds what upstream lacks (see README):
* SponsorBlock: "Undo" button for ~5 s after an automatic skip, and the
  sponsor segment painted yellow on the seek bar (self-promo takes green).
* DeArrow enabled by default (titles + thumbnails); users can still turn it off.
* Quest 3 / 3S / Pro default to 4K VP9 60 fps without relying on codec probing.
* Subtitles 1.35x by default so they are readable at headset distance.

Every anchor in every file is verified before anything is written; any
mismatch (upstream changed, or patch already applied) aborts untouched.
"""
from pathlib import Path
import argparse

COMMON = "common/src/main/java/com/liskovsoft/smartyoutubetv2/common"
CONTROLLER = COMMON + "/app/models/playback/controllers/SponsorBlockController.java"
SB_DATA = COMMON + "/prefs/SponsorBlockData.java"
DEARROW = COMMON + "/prefs/DeArrowData.java"
PLAYER = COMMON + "/prefs/PlayerData.java"
STRINGS = "common/src/main/res/values/strings.xml"
STRINGS_ES = "common/src/main/res/values-es/strings.xml"

UNDO_MS = 5_000
SUBTITLE_SCALE = "1.35f"
QUEST_MODELS = ("Quest 3", "Quest 3S", "Quest Pro")

CHANGES = {
    CONTROLLER: [
        ("    private String mVideoId;\n",
         "    private String mVideoId;\n    private static final long UNDO_SKIP_MS = %d; // SmartTube VR\n" % UNDO_MS),
        ("""        if (!isDurationIgnored) {
            if (type == SponsorBlockData.ACTION_SKIP_ONLY || getPlayer().isInPIPMode() || Utils.isScreenOff(getContext()) || isEmbedPlayer()) {""",
         """        if (!isDurationIgnored) {
            boolean isBackground = getPlayer().isInPIPMode() || Utils.isScreenOff(getContext()) || isEmbedPlayer();
            boolean willSkip = mLastSkipPosMs != skipPosMs && (isBackground
                    || type == SponsorBlockData.ACTION_SKIP_ONLY || type == SponsorBlockData.ACTION_SKIP_WITH_TOAST);
            long undoPosMs = getPlayer().getPositionMs();
            if (type == SponsorBlockData.ACTION_SKIP_ONLY || isBackground) {"""),
        ("""                confirmSkip(skipPosMs, skipMessage);
            }
        }
""",
         """                confirmSkip(skipPosMs, skipMessage);
            }
            if (willSkip && !isBackground) {
                showUndoSkip(foundSegments, undoPosMs, skipMessage);
            }
        }
"""),
        ("    private void closeTransparentDialog() {",
         """    /**
     * SmartTube VR: after an automatic skip, offer a laser-friendly "Undo" button
     * for a few seconds. Undo seeks back and keeps those segments from re-skipping.
     */
    private void showUndoSkip(List<SponsorSegment> segments, long undoPosMs, String category) {
        AppDialogPresenter dialogPresenter = AppDialogPresenter.instance(getContext());

        if (getPlayer() == null || dialogPresenter.isDialogShown() || getPlayer().isSuggestionsShown()) {
            return; // Don't cover another dialog; the toast already informed the user.
        }

        OptionItem undoOption = UiOptionItem.from(
                getContext().getString(R.string.sponsor_undo_skip, category),
                option -> {
                    dialogPresenter.goBack();
                    if (mActiveSegments != null && segments != null) {
                        mActiveSegments.removeAll(segments);
                    }
                    if (getPlayer() != null) {
                        setPositionMs(undoPosMs);
                    }
                }
        );

        dialogPresenter.appendSingleButton(undoOption);
        dialogPresenter.setCloseTimeoutMs(UNDO_SKIP_MS);
        dialogPresenter.enableTransparent(true);
        dialogPresenter.enableOverlay(true);
        dialogPresenter.enableExpandable(false);
        dialogPresenter.setId(CONTENT_BLOCK_ID);
        dialogPresenter.showDialog(getContext().getString(R.string.content_block_provider));
    }

    private void closeTransparentDialog() {"""),
    ],
    SB_DATA: [
        ("mSegmentColorMapping.put(SponsorSegment.CATEGORY_SPONSOR, R.color.green);",
         "mSegmentColorMapping.put(SponsorSegment.CATEGORY_SPONSOR, R.color.yellow); // SmartTube VR"),
        ("mSegmentColorMapping.put(SponsorSegment.CATEGORY_SELF_PROMO, R.color.yellow);",
         "mSegmentColorMapping.put(SponsorSegment.CATEGORY_SELF_PROMO, R.color.green);"),
    ],
    DEARROW: [
        ("        return getBoolean(0);\n", "        return getBoolean(0, true); // SmartTube VR: on by default\n"),
        ("        return getBoolean(1);\n", "        return getBoolean(1, true);\n"),
        ("        return getBoolean(2);\n", "        return getBoolean(2, true);\n"),
    ],
    PLAYER: [
        ('        mDefaultVideoFormats.put("SHIELD Android TV", FormatItem.VIDEO_4K_VP9_60);\n',
         '        mDefaultVideoFormats.put("SHIELD Android TV", FormatItem.VIDEO_4K_VP9_60);\n'
         + "".join('        mDefaultVideoFormats.put("%s", FormatItem.VIDEO_4K_VP9_60); // SmartTube VR\n' % m
                   for m in QUEST_MODELS)),
        ("mSubtitleScale = Helpers.parseFloat(split, 39, 1.0f);",
         "mSubtitleScale = Helpers.parseFloat(split, 39, %s); // SmartTube VR: headset size" % SUBTITLE_SCALE),
    ],
    STRINGS: [
        ("</resources>", '    <string name="sponsor_undo_skip">Undo: %s</string>\n</resources>'),
    ],
    STRINGS_ES: [
        ("</resources>", '    <string name="sponsor_undo_skip">Deshacer: %s</string>\n</resources>'),
    ],
}


def plan(root):
    """Verify every anchor in every file; return new contents without writing."""
    out = {}
    for rel, changes in CHANGES.items():
        path = root / rel
        if not path.is_file():
            raise SystemExit(f"PATCH FAILED: missing {path}")
        raw = path.read_bytes().decode("utf-8")
        crlf = "\r\n" in raw
        text = raw.replace("\r\n", "\n")
        if "sponsor_undo_skip" in text or "// SmartTube VR" in text:
            raise SystemExit(f"PATCH FAILED: {path} already patched")
        for old, new in changes:
            count = text.count(old)
            if count != 1:
                raise SystemExit(f"PATCH FAILED: {path}: expected one anchor, found {count}: {old[:80]!r}")
            text = text.replace(old, new, 1)
        out[path] = text.replace("\n", "\r\n") if crlf else text
    return out


def main(root):
    for path, text in plan(Path(root)).items():
        path.write_bytes(text.encode("utf-8"))  # keep upstream line endings
        print("PATCHED", path)
    print("SmartTube VR Quest media patch complete.")


if __name__ == "__main__":
    parser = argparse.ArgumentParser()
    parser.add_argument("--source", required=True, type=Path)
    main(parser.parse_args().source)
