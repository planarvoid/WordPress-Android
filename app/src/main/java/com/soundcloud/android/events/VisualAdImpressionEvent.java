package com.soundcloud.android.events;

import com.google.auto.value.AutoValue;
import com.soundcloud.android.ads.AdData;
import com.soundcloud.android.ads.AudioAd;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playback.TrackSourceInfo;
import com.soundcloud.java.optional.Optional;

import android.net.Uri;

import java.util.List;

@AutoValue
public abstract class VisualAdImpressionEvent extends TrackingEvent {
    public static final String EVENT_NAME = "impression";

    public enum ImpressionName {
        COMPANION_DISPLAY("companion_display");

        private final String key;

        ImpressionName(String key) {
            this.key = key;
        }

        public String key() {
            return key;
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
                                                     AdData.MonetizationType.AUDIO);
    }

    public abstract String userUrn();

    public abstract String trackUrn();

    public abstract String originScreen();

    public abstract Optional<Urn> adUrn();

    public abstract Optional<Uri> adArtworkUrl();

    public abstract List<String> impressionUrls();

    public abstract ImpressionName impressionName();

    public abstract AdData.MonetizationType monetizationType();

    @Override
    public VisualAdImpressionEvent putReferringEvent(ReferringEvent referringEvent) {
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
