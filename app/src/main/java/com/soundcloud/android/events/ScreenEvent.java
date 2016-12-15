package com.soundcloud.android.events;

import com.google.auto.value.AutoValue;
import com.soundcloud.android.analytics.SearchQuerySourceInfo;
import com.soundcloud.android.explore.ExploreGenre;
import com.soundcloud.android.main.Screen;
import com.soundcloud.android.model.Urn;
import com.soundcloud.java.optional.Optional;

@AutoValue
public abstract class ScreenEvent extends NewTrackingEvent {

    public static final String KIND = "screen";

    public String getKind() {
        return KIND;
    }

    public abstract String screen();

    public abstract Optional<String> genre();

    public abstract Optional<Urn> queryUrn();

    public abstract Optional<Urn> pageUrn();


    public static ScreenEvent create(Screen screen) {
        return create(screen.get());
    }

    public static ScreenEvent create(String screen) {
        return builder(screen).build();
    }

    public static ScreenEvent create(String screen, SearchQuerySourceInfo searchQuerySourceInfo) {
        return create(screen, searchQuerySourceInfo.getQueryUrn());
    }

    public static ScreenEvent create(String screen, Urn queryUrn) {
        return builder(screen).queryUrn(Optional.of(queryUrn)).build();
    }

    public static ScreenEvent create(String screen, ExploreGenre genre) {
        return builder(screen).genre(Optional.of(genre.getTitle())).build();
    }

    public static ScreenEvent create(Screen screen, Urn pageUrn) {
        return builder(screen.get()).pageUrn(Optional.of(pageUrn)).build();
    }

    private static Builder builder(String screen) {
        return new AutoValue_ScreenEvent.Builder().id(defaultId())
                                                  .timestamp(defaultTimestamp())
                                                  .referringEvent(Optional.absent())
                                                  .screen(screen)
                                                  .genre(Optional.absent())
                                                  .queryUrn(Optional.absent())
                                                  .pageUrn(Optional.absent());
    }

    @Override
    public ScreenEvent putReferringEvent(ReferringEvent referringEvent) {
        return new AutoValue_ScreenEvent.Builder(this).referringEvent(Optional.of(referringEvent)).build();
    }

    @AutoValue.Builder
    public abstract static class Builder {
        public abstract Builder id(String id);

        public abstract Builder timestamp(long timestamp);

        public abstract Builder referringEvent(Optional<ReferringEvent> referringEvent);

        public abstract Builder screen(String screen);

        public abstract Builder genre(Optional<String> genre);

        public abstract Builder queryUrn(Optional<Urn> queryUrn);

        public abstract Builder pageUrn(Optional<Urn> pageUrn);

        public abstract ScreenEvent build();
    }
}
