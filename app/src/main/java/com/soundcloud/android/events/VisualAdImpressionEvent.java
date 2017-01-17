package com.soundcloud.android.events;

import com.google.auto.value.AutoValue;
import com.soundcloud.android.ads.AudioAd;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playback.TrackSourceInfo;
import com.soundcloud.java.optional.Optional;

import android.net.Uri;

import java.util.List;

@AutoValue
public abstract class VisualAdImpressionEvent extends NewTrackingEvent {
    public enum ImpressionName {
        COMPANION_DISPLAY("companion_display");

        private final String name;

        ImpressionName(String name) {
            this.name = name;
        }

        @Override
        public String toString() {
            return name;
        }
    }

    public enum MonetizationType {
        AUDIO_AD("audio_ad");

        private final String type;

        MonetizationType(String type) {
            this.type = type;
        }

        @Override
        public String toString() {
            return type;
        }
    }

    public static VisualAdImpressionEvent create(AudioAd adData, Urn userUrn, TrackSourceInfo sessionSource) {
        return new AutoValue_VisualAdImpressionEvent(defaultId(),
                                                     defaultTimestamp(),
                                                     Optional.absent(),
                                                     userUrn.toString(),
                                                     adData.getMonetizableTrackUrn().toString(),
                                                     sessionSource.getOriginScreen(),
                                                     adData.getCompanionAdUrn(),
                                                     adData.getCompanionImageUrl(),
                                                     adData.getCompanionImpressionUrls(),
                                                     ImpressionName.COMPANION_DISPLAY,
                                                     MonetizationType.AUDIO_AD);
    }

    public abstract String userUrn();

    public abstract String trackUrn();

    public abstract String originScreen();

    public abstract Optional<Urn> adUrn();

    public abstract Optional<Uri> adArtworkUrl();

    public abstract List<String> impressionUrls();

    public abstract ImpressionName impressionName();

    public abstract MonetizationType monetizationType();

    @Override
    public TrackingEvent putReferringEvent(ReferringEvent referringEvent) {
        return new AutoValue_VisualAdImpressionEvent(id(),
                                                     timestamp(),
                                                     Optional.of(referringEvent),
                                                     userUrn(),
                                                     trackUrn(),
                                                     originScreen(),
                                                     adUrn(),
                                                     adArtworkUrl(),
                                                     impressionUrls(),
                                                     impressionName(),
                                                     monetizationType());
    }
}
