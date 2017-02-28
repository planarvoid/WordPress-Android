package com.soundcloud.android.events;


import static com.soundcloud.android.events.AdOverlayTrackingEvent.Type.TYPE_AUDIO_AD;
import static com.soundcloud.android.events.AdOverlayTrackingEvent.Type.TYPE_INTERSTITIAL;
import static com.soundcloud.android.events.AdOverlayTrackingEvent.Type.TYPE_LEAVE_BEHIND;

import com.google.auto.value.AutoValue;
import com.soundcloud.android.ads.InterstitialAd;
import com.soundcloud.android.ads.LeaveBehindAd;
import com.soundcloud.android.ads.OverlayAdData;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playback.TrackSourceInfo;
import com.soundcloud.java.optional.Optional;
import org.jetbrains.annotations.Nullable;

import android.net.Uri;
import android.support.annotation.VisibleForTesting;

import java.util.List;

@AutoValue
public abstract class AdOverlayTrackingEvent extends TrackingEvent {

    public static final String CLICKTHROUGH_FORMAT = "clickthrough::%s";

    public enum EventName {

        KIND_IMPRESSION("impression"),
        KIND_CLICK("click");
        private final String key;

        EventName(String key) {
            this.key = key;
        }

        public String key() {
            return key;
        }
    }

    public abstract EventName eventName();

    public abstract List<String> trackingUrls();

    public abstract Urn user();

    public abstract Urn monetizableTrack();

    public abstract String adArtworkUrl();

    public abstract Optional<String> originScreen();

    public abstract Urn adUrn();

    public abstract Optional<Type> monetizationType();

    public abstract Optional<String> clickName();

    public abstract Optional<Uri> clickTarget();

    public abstract Optional<Urn> clickObject();

    public abstract Optional<Type> impressionName();

    public abstract Optional<Urn> impressionObject();

    public enum Type {

        TYPE_LEAVE_BEHIND("leave_behind"),
        TYPE_INTERSTITIAL("interstitial"),
        TYPE_AUDIO_AD("audio_ad");
        private final String key;

        Type(String key) {
            this.key = key;
        }

        public String key() {
            return key;
        }
    }


    private static AdOverlayTrackingEvent.Builder create(long timeStamp,
                                                         EventName eventName,
                                                         OverlayAdData adData,
                                                         Urn monetizableTrack,
                                                         Urn user,
                                                         List<String> trackingUrls,
                                                         @Nullable TrackSourceInfo trackSourceInfo) {
        final Optional<String> screen = getNonNullOriginScreenValue(trackSourceInfo);
        Optional<Type> monetizationType = Optional.absent();
        if (adData instanceof LeaveBehindAd) {
            monetizationType = Optional.of(TYPE_AUDIO_AD);
        } else if (adData instanceof InterstitialAd) {
            monetizationType = Optional.of(TYPE_INTERSTITIAL);
        }
        return new AutoValue_AdOverlayTrackingEvent.Builder().id(defaultId())
                                                             .timestamp(timeStamp)
                                                             .referringEvent(Optional.absent())
                                                             .eventName(eventName)
                                                             .trackingUrls(trackingUrls)
                                                             .user(user)
                                                             .monetizableTrack(monetizableTrack)
                                                             .adArtworkUrl(adData.getImageUrl())
                                                             .originScreen(screen)
                                                             .adUrn(adData.getAdUrn())
                                                             .monetizationType(monetizationType)
                                                             .clickName(Optional.absent())
                                                             .clickTarget(Optional.absent())
                                                             .clickObject(Optional.absent())
                                                             .impressionObject(Optional.absent())
                                                             .impressionName(Optional.absent());
    }

    private static Optional<String> getNonNullOriginScreenValue(@Nullable TrackSourceInfo trackSourceInfo) {
        if (trackSourceInfo != null) {
            return Optional.of(trackSourceInfo.getOriginScreen());
        }
        return Optional.absent();
    }

    public static AdOverlayTrackingEvent forClick(OverlayAdData adData, Urn track, Urn user, @Nullable TrackSourceInfo sourceInfo) {
        return forClick(defaultTimestamp(), adData, track, user, sourceInfo);
    }

    public static AdOverlayTrackingEvent forImpression(OverlayAdData adData, Urn track, Urn user, @Nullable TrackSourceInfo sourceInfo) {
        return forImpression(defaultTimestamp(), adData, track, user, sourceInfo);
    }

    @VisibleForTesting
    public static AdOverlayTrackingEvent forImpression(long timeStamp,
                                                       OverlayAdData adData,
                                                       Urn track,
                                                       Urn user,
                                                       TrackSourceInfo sourceInfo) {
        final List<String> trackingUrls = adData.getImpressionUrls();
        return AdOverlayTrackingEvent.create(timeStamp, EventName.KIND_IMPRESSION, adData, track, user, trackingUrls, sourceInfo)
                                     .impressionName(getImpressionName(adData))
                                     .impressionObject(getImpressionObject(adData, track))
                                     .build();
    }

    @VisibleForTesting
    public static AdOverlayTrackingEvent forClick(long timestamp,
                                                  OverlayAdData adData,
                                                  Urn track,
                                                  Urn user,
                                                  TrackSourceInfo sourceInfo) {
        final List<String> trackingUrls = adData.getClickUrls();
        return AdOverlayTrackingEvent.create(timestamp, EventName.KIND_CLICK, adData, track, user, trackingUrls, sourceInfo)
                                     .clickName(getClickName(adData))
                                     .clickTarget(Optional.of(adData.getClickthroughUrl()))
                                     .clickObject(getClickObject(adData))
                                     .build();
    }

    @Override
    public AdOverlayTrackingEvent putReferringEvent(ReferringEvent referringEvent) {
        return null;
    }

    private static Optional<String> getClickName(OverlayAdData adData) {
        if (adData instanceof LeaveBehindAd) {
            return Optional.of(String.format(CLICKTHROUGH_FORMAT, TYPE_LEAVE_BEHIND.key()));
        } else if (adData instanceof InterstitialAd) {
            return Optional.of(String.format(CLICKTHROUGH_FORMAT, TYPE_INTERSTITIAL.key()));
        }
        return Optional.absent();
    }

    private static Optional<Type> getImpressionName(OverlayAdData adData) {
        if (adData instanceof LeaveBehindAd) {
            return Optional.of(TYPE_LEAVE_BEHIND);
        } else if (adData instanceof InterstitialAd) {
            return Optional.of(TYPE_INTERSTITIAL);
        }
        return Optional.absent();
    }

    private static Optional<Urn> getClickObject(OverlayAdData adData) {
        if (adData instanceof LeaveBehindAd) {
            return Optional.of(((LeaveBehindAd) adData).getAudioAdUrn());
        }
        return Optional.absent();
    }

    private static Optional<Urn> getImpressionObject(OverlayAdData adData, Urn monetizableTrack) {
        if (adData instanceof LeaveBehindAd) {
            final Urn audioAdUrn = ((LeaveBehindAd) adData).getAudioAdUrn();
            return Optional.of(audioAdUrn);
        } else if (adData instanceof InterstitialAd) {
            return Optional.of(monetizableTrack);
        }
        return Optional.absent();
    }

    @AutoValue.Builder
    public abstract static class Builder {
        public abstract Builder id(String id);

        public abstract Builder timestamp(long timestamp);

        public abstract Builder referringEvent(Optional<ReferringEvent> referringEvent);

        public abstract Builder eventName(EventName eventName);

        public abstract Builder trackingUrls(List<String> trackingUrls);

        public abstract Builder user(Urn user);

        public abstract Builder monetizableTrack(Urn monetizableTrack);

        public abstract Builder adArtworkUrl(String adArtworkUrl);

        public abstract Builder originScreen(Optional<String> originScreen);

        public abstract Builder adUrn(Urn adUrn);

        public abstract Builder monetizationType(Optional<Type> monetizationType);

        public abstract Builder clickName(Optional<String> clickName);

        public abstract Builder clickTarget(Optional<Uri> clickTarget);

        public abstract Builder clickObject(Optional<Urn> clickObject);

        public abstract Builder impressionName(Optional<Type> impressionName);

        public abstract Builder impressionObject(Optional<Urn> impressionObject);

        public abstract AdOverlayTrackingEvent build();
    }
}
