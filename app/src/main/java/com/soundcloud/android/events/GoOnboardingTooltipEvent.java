package com.soundcloud.android.events;

import com.google.auto.value.AutoValue;
import com.soundcloud.android.introductoryoverlay.IntroductoryOverlayKey;
import com.soundcloud.java.optional.Optional;

@AutoValue
public abstract class GoOnboardingTooltipEvent extends TrackingEvent {

    public static GoOnboardingTooltipEvent forListenOfflineLikes() {
        return new AutoValue_GoOnboardingTooltipEvent(defaultId(), defaultTimestamp(), Optional.absent(), IntroductoryOverlayKey.LISTEN_OFFLINE_LIKES);
    }

    public static GoOnboardingTooltipEvent forListenOfflinePlaylist() {
        return new AutoValue_GoOnboardingTooltipEvent(defaultId(), defaultTimestamp(), Optional.absent(), IntroductoryOverlayKey.LISTEN_OFFLINE_PLAYLIST);
    }

    public static GoOnboardingTooltipEvent forSearchGoPlus() {
        return new AutoValue_GoOnboardingTooltipEvent(defaultId(), defaultTimestamp(), Optional.absent(), IntroductoryOverlayKey.SEARCH_GO_PLUS);
    }

    public static GoOnboardingTooltipEvent forOfflineSettings() {
        return new AutoValue_GoOnboardingTooltipEvent(defaultId(), defaultTimestamp(), Optional.absent(), IntroductoryOverlayKey.OFFLINE_SETTINGS);
    }

    public abstract String tooltipName();

    @Override
    public GoOnboardingTooltipEvent putReferringEvent(ReferringEvent referringEvent) {
        return new AutoValue_GoOnboardingTooltipEvent(this.id(), this.timestamp(), Optional.of(referringEvent), this.tooltipName());
    }
}
