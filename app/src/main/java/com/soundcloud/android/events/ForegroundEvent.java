package com.soundcloud.android.events;

import com.google.auto.value.AutoValue;
import com.soundcloud.android.analytics.Referrer;
import com.soundcloud.android.main.Screen;
import com.soundcloud.android.model.Urn;
import com.soundcloud.java.optional.Optional;

@AutoValue
public abstract class ForegroundEvent extends NewTrackingEvent {
    public static final String KIND_OPEN = "open";

    @Override
    public String getKind() {
        return KIND_OPEN;
    }

    public abstract String pageName();

    public abstract Optional<Urn> pageUrn();

    public abstract String referrer();

    public static ForegroundEvent open(Screen screen, Referrer referrer) {
        return open(screen, referrer.value());
    }

    public static ForegroundEvent open(Screen screen, String referrer) {
        return open(screen, referrer, null);
    }

    public static ForegroundEvent open(Screen screen, String referrer, Urn urn) {
        return new AutoValue_ForegroundEvent.Builder().id(defaultId())
                                                      .timestamp(defaultTimestamp())
                                                      .referringEvent(Optional.absent())
                                                      .pageName(screen.get())
                                                      .pageUrn(Optional.fromNullable(urn))
                                                      .referrer(referrer)
                                                      .build();
    }

    @Override
    public TrackingEvent putReferringEvent(ReferringEvent referringEvent) {
        return new AutoValue_ForegroundEvent.Builder(this).referringEvent(Optional.of(referringEvent)).build();
    }

    @AutoValue.Builder
    public abstract static class Builder {
        public abstract Builder id(String id);

        public abstract Builder timestamp(long timestamp);

        public abstract Builder referringEvent(Optional<ReferringEvent> referringEvent);

        public abstract Builder pageName(String pageName);

        public abstract Builder pageUrn(Optional<Urn> pageUrn);

        public abstract Builder referrer(String referrer);

        public abstract ForegroundEvent build();
    }
}
