package com.soundcloud.android.events;

import com.google.auto.value.AutoValue;
import com.soundcloud.android.introductoryoverlay.IntroductoryOverlayKey;
import com.soundcloud.android.main.Screen;
import com.soundcloud.android.model.Urn;
import com.soundcloud.java.optional.Optional;

@AutoValue
public abstract class GoOnboardingTooltipEvent extends TrackingEvent {

    public static final String EVENT_NAME = "impression";

    private static final String IMPRESSION_CATEGORY = "consumer_subs";
    private static final String IMPRESSION_NAME_COLLECTION_OFFLINE_ONBOARDING = "tooltip::save_offline_content";
    private static final String IMPRESSION_NAME_LISTEN_OFFLINE_LIKES = "tooltip::save_likes";
    private static final String IMPRESSION_NAME_LISTEN_OFFLINE_PLAYLIST = "tooltip::save_playlist_or_album";
    private static final String IMPRESSION_NAME_SEARCH_GO_PLUS = "tooltip::go_plus_marker";
    private static final String IMPRESSION_NAME_OFFLINE_SETTINGS = "tooltip::offline_settings";

    public static GoOnboardingTooltipEvent forCollectionImpression() {
        return create(Optional.absent(),
                      Screen.COLLECTIONS.get(),
                      Optional.absent(),
                      IMPRESSION_NAME_COLLECTION_OFFLINE_ONBOARDING);
    }

    public static GoOnboardingTooltipEvent forListenOfflineLikes() {
        return create(Optional.of(IntroductoryOverlayKey.LISTEN_OFFLINE_LIKES),
                      Screen.LIKES.get(),
                      Optional.absent(),
                      IMPRESSION_NAME_LISTEN_OFFLINE_LIKES);
    }

    public static GoOnboardingTooltipEvent forListenOfflinePlaylist(Urn playlistUrn) {
        return create(Optional.of(IntroductoryOverlayKey.LISTEN_OFFLINE_PLAYLIST),
                      Screen.PLAYLIST_DETAILS.get(),
                      Optional.of(playlistUrn.toString()),
                      IMPRESSION_NAME_LISTEN_OFFLINE_PLAYLIST);
    }

    public static GoOnboardingTooltipEvent forSearchGoPlus() {
        return create(Optional.of(IntroductoryOverlayKey.SEARCH_GO_PLUS),
                      Screen.SEARCH_MAIN.get(),
                      Optional.absent(),
                      IMPRESSION_NAME_SEARCH_GO_PLUS);
    }

    public static GoOnboardingTooltipEvent forOfflineSettings() {
        return create(Optional.of(IntroductoryOverlayKey.OFFLINE_SETTINGS),
                      Screen.MORE.get(),
                      Optional.absent(),
                      IMPRESSION_NAME_OFFLINE_SETTINGS);
    }

    private static GoOnboardingTooltipEvent create(Optional<String> tooltipName, String pageName, Optional<String> pageUrn, String impressionName) {
        return new AutoValue_GoOnboardingTooltipEvent(defaultId(),
                                                      defaultTimestamp(),
                                                      Optional.absent(),
                                                      tooltipName,
                                                      pageName,
                                                      pageUrn,
                                                      IMPRESSION_CATEGORY,
                                                      impressionName);
    }

    public abstract Optional<String> tooltipName();
    public abstract String pageName();
    public abstract Optional<String> pageUrn();
    public abstract String impressionCategory();
    public abstract String impressionName();

    @Override
    public GoOnboardingTooltipEvent putReferringEvent(ReferringEvent referringEvent) {
        return new AutoValue_GoOnboardingTooltipEvent(this.id(),
                                                      this.timestamp(),
                                                      Optional.of(referringEvent),
                                                      this.tooltipName(),
                                                      this.pageName(),
                                                      this.pageUrn(),
                                                      this.impressionCategory(),
                                                      this.impressionName());
    }
}
