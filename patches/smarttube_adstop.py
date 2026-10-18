#!/usr/bin/env python3
"""Apply a small, reviewable patch to upstream yuliskov/SmartTube.
The original SmartTube source, playback, and services remain the base.
"""
from pathlib import Path
import argparse

def update(path, changes):
    text = path.read_text(encoding="utf-8")
    for old, new in changes:
        count = text.count(old)
        if count != 1:
            raise SystemExit(f"PATCH FAILED: {path}: expected one anchor, found {count}: {old[:80]!r}")
        text = text.replace(old, new, 1)
    path.write_text(text, encoding="utf-8")
    print("PATCHED", path)

def append_strings(file, lines):
    src = file.read_text(encoding="utf-8")
    assert src.count("</resources>") == 1
    for name in ("adstop_presets", "adstop_balanced", "adstop_aggressive", "dearrow_clean_preset", "dearrow_original_preset"):
        if 'name="' + name + '"' in src:
            raise SystemExit(f"String collision in {file}: {name}")
    file.write_text(src.replace("</resources>", "\n" + lines + "\n</resources>"), encoding="utf-8")
    print("PATCHED", file)

def main(root):
    base = root / "common/src/main/java/com/liskovsoft/smartyoutubetv2/common"
    pref = base / "prefs/SponsorBlockData.java"
    update(pref, [
        ("    public boolean isActionsEnabled() {",
         """    /**
     * AdStop profile: configures the *existing* SponsorBlock engine.
     * Does not install a VPN or affect ads in other applications.
     * A manual action can always override these presets afterwards.
     */
    public void applyAdStopPreset(boolean aggressive) {
        mIsSponsorBlockEnabled = true;
        for (SegmentAction action : mActions) {
            String category = action.segmentCategory;
            if (SponsorSegment.CATEGORY_SPONSOR.equals(category)
                    || SponsorSegment.CATEGORY_SELF_PROMO.equals(category)
                    || SponsorSegment.CATEGORY_INTERACTION.equals(category)) {
                action.actionType = ACTION_SKIP_ONLY;
            } else if (SponsorSegment.CATEGORY_INTRO.equals(category)
                    || SponsorSegment.CATEGORY_OUTRO.equals(category)
                    || SponsorSegment.CATEGORY_PREVIEW_RECAP.equals(category)) {
                action.actionType = aggressive ? ACTION_SKIP_ONLY : ACTION_SKIP_WITH_TOAST;
            } else if (SponsorSegment.CATEGORY_FILLER.equals(category)
                    || SponsorSegment.CATEGORY_MUSIC_OFF_TOPIC.equals(category)) {
                action.actionType = aggressive ? ACTION_SKIP_ONLY : ACTION_DO_NOTHING;
            } else {
                // Highlights can be important and should not be removed.
                action.actionType = ACTION_DO_NOTHING;
            }
        }
        mIgnoredDurationMs = aggressive ? 0L : 5_000L;
        mColorCategories.clear();
        mColorCategories.addAll(mAllCategories);
        persistState();
    }

    public boolean isActionsEnabled() {"""),
        ("""    public void stopExcludingChannel(String channelId) {
        mExcludedChannels.remove(channelId);
        persistState();
    }""",
         """    public void stopExcludingChannel(String channelId) {
        if (channelId != null && mExcludedChannels.remove(channelId)) {
            persistState();
        }
    }""")
    ])

    sponsor = base / "app/presenters/settings/SponsorBlockSettingsPresenter.java"
    update(sponsor, [
        ("        appendSponsorBlockSwitch(settingsPresenter);\n",
         "        appendSponsorBlockSwitch(settingsPresenter);\n        appendAdStopPresets(settingsPresenter);\n"),
        ("    private void appendActionsCategory(AppDialogPresenter settingsPresenter) {",
         """    private void appendAdStopPresets(AppDialogPresenter settingsPresenter) {
        List<OptionItem> options = new ArrayList<>();
        options.add(UiOptionItem.from(getContext().getString(R.string.adstop_balanced),
                item -> mSponsorBlockData.applyAdStopPreset(false)));
        options.add(UiOptionItem.from(getContext().getString(R.string.adstop_aggressive),
                item -> mSponsorBlockData.applyAdStopPreset(true)));
        settingsPresenter.appendStringsCategory(getContext().getString(R.string.adstop_presets), options);
    }

    private void appendActionsCategory(AppDialogPresenter settingsPresenter) {""")
    ])

    dearrow = base / "app/presenters/settings/DeArrowSettingsPresenter.java"
    update(dearrow, [
        ("        appendSwitches(settingsPresenter);\n",
         "        appendQuickPresets(settingsPresenter);\n        appendSwitches(settingsPresenter);\n"),
        ("    private void appendThumbQuality(AppDialogPresenter settingsPresenter) {",
         """    private void appendQuickPresets(AppDialogPresenter settingsPresenter) {
        settingsPresenter.appendSingleButton(UiOptionItem.from(
                getContext().getString(R.string.dearrow_clean_preset), item -> {
                    mMainUIData.setUnlocalizedTitlesEnabled(false);
                    mDeArrowData.setDeArrowEnabled(true);
                    mDeArrowData.setReplaceTitlesEnabled(true);
                    mDeArrowData.setReplaceThumbnailsEnabled(true);
                }));
        settingsPresenter.appendSingleButton(UiOptionItem.from(
                getContext().getString(R.string.dearrow_original_preset), item -> {
                    mDeArrowData.setReplaceTitlesEnabled(false);
                    mDeArrowData.setReplaceThumbnailsEnabled(false);
                    mDeArrowData.setDeArrowEnabled(false);
                }));
    }

    private void appendThumbQuality(AppDialogPresenter settingsPresenter) {""")
    ])

    processor = base / "misc/DeArrowProcessor.java"
    update(processor, [
        ("        List<String> videoIds = getVideoIds(videoGroup);\n",
         """        List<String> videoIds = getVideoIds(videoGroup);
        if (videoIds.isEmpty()) {
            return; // Avoid useless network requests for already processed groups.
        }
"""),
        ("                    Video video = videoGroup.findVideoById(deArrowData.getVideoId());\n",
         """                    if (deArrowData == null || deArrowData.getVideoId() == null) {
                        return;
                    }
                    Video video = videoGroup.findVideoById(deArrowData.getVideoId());
                    if (video == null) {
                        return; // API may return an item no longer present in this group.
                    }
"""),
        ("""                    Log.d(TAG, "DeArrow cannot process the video");
""",
         """                    Log.d(TAG, "DeArrow cannot process the video");
                    // Allow retry after an API/network error rather than marking
                    // the entire batch permanently processed.
                    for (Video video : videoGroup.getVideos()) {
                        if (video != null && videoIds.contains(video.videoId)) {
                            video.deArrowProcessed = false;
                        }
                    }
"""),
        ("""            if (video.deArrowProcessed) {
""",
         """            if (video == null || video.videoId == null || video.videoId.isEmpty()
                    || video.deArrowProcessed) {
""")
    ])

    append_strings(root / "common/src/main/res/values/strings.xml", """    <string name="adstop_presets">AdStop: SponsorBlock quick profiles</string>
    <string name="adstop_balanced">Balanced: skip sponsors and promotions, notify for chapters</string>
    <string name="adstop_aggressive">Aggressive: also skip intro, recap, outro and filler</string>
    <string name="dearrow_clean_preset">DeArrow: clean titles and thumbnails</string>
    <string name="dearrow_original_preset">DeArrow: use original titles and thumbnails</string>""")
    append_strings(root / "common/src/main/res/values-es/strings.xml", """  <string name="adstop_presets">AdStop: perfiles de SponsorBlock</string>
  <string name="adstop_balanced">Equilibrado: saltar patrocinadores y promociones; avisar en capítulos</string>
  <string name="adstop_aggressive">Intensivo: saltar intros, resúmenes, finales y relleno</string>
  <string name="dearrow_clean_preset">DeArrow: limpiar títulos y miniaturas</string>
  <string name="dearrow_original_preset">DeArrow: recuperar títulos y miniaturas originales</string>""")

    gradle = root / "smarttubetv/build.gradle"
    update(gradle, [
        ("        debug {\n            if (keystorePropertiesFile.exists()) {",
         """        debug {
            // Do not overwrite the official SmartTube installation.
            applicationIdSuffix ".vr"
            if (keystorePropertiesFile.exists()) {""")
    ])

    # Install as its own app named "SmartTube VR" (package app.smarttube.fdroid.vr).
    update(root / "smarttubetv/src/stfdroid/res/values/strings.xml", [
        ('name="app_name"><![CDATA[SmartTube F-Droid]]>', 'name="app_name"><![CDATA[SmartTube VR]]>'),
        ('name="browse_title"><![CDATA[SmartTube F-Droid]]>', 'name="browse_title"><![CDATA[SmartTube VR]]>'),
    ])

    manifest = root / "smarttubetv/src/main/AndroidManifest.xml"
    update(manifest, [
        ('android:name="android.software.leanback"\n        android:required="true" />',
         'android:name="android.software.leanback"\n        android:required="false"\n        tools:node="replace" />')
    ])

    print("SmartTube original-source patch complete.")

if __name__ == "__main__":
    parser = argparse.ArgumentParser()
    parser.add_argument("--source", required=True, type=Path)
    args = parser.parse_args()
    main(args.source)
