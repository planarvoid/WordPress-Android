package com.soundcloud.android.events;

import static com.soundcloud.android.events.CollectionEvent.FilterTag.ALL;
import static com.soundcloud.android.events.CollectionEvent.FilterTag.CREATED;
import static com.soundcloud.android.events.CollectionEvent.FilterTag.LIKED;
import static com.soundcloud.android.events.CollectionEvent.Target.SORT_RECENT;
import static com.soundcloud.android.events.CollectionEvent.Target.SORT_TITLE;

import com.google.auto.value.AutoValue;
import com.soundcloud.android.collection.playlists.PlaylistsOptions;
import com.soundcloud.android.main.Screen;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playback.DiscoverySource;
import com.soundcloud.java.optional.Optional;

@AutoValue
public abstract class CollectionEvent extends NewTrackingEvent {

    public static final String COLLECTION_CATEGORY = "collection";

    public enum ClickName {
        ITEM_NAVIGATION("item_navigation"),
        SET("filter_sort::set"),
        CLEAR("filter_sort::clear");
        private final String key;

        ClickName(String key) {
            this.key = key;
        }

        @Override
        public String toString() {
            return key;
        }
    }

    public enum FilterTag {
        ALL("filter:all"),
        CREATED("filter:created"),
        LIKED("filter:liked");
        private final String key;

        FilterTag(String key) {
            this.key = key;
        }

        @Override
        public String toString() {
            return key;
        }
    }

    public enum Target {
        SORT_TITLE("sort:title"),
        SORT_RECENT("sort:recent");
        private final String key;

        Target(String key) {
            this.key = key;
        }

        @Override
        public String toString() {
            return key;
        }
    }

    public abstract ClickName clickName();

    public abstract Optional<String> clickCategory();

    public abstract Optional<String> object();

    public abstract Optional<Target> target();

    public abstract String pageName();

    public abstract Optional<DiscoverySource> source();

    @Override
    public TrackingEvent putReferringEvent(ReferringEvent referringEvent) {
        return null;
    }

    public static CollectionEvent forClearFilter() {
        return builder(ClickName.CLEAR, Screen.COLLECTIONS.get()).clickCategory(Optional.of(COLLECTION_CATEGORY)).build();
    }

    public static CollectionEvent forFilter(PlaylistsOptions currentOptions) {
        final FilterTag objectTag = getObjectTag(currentOptions);
        final Target target = currentOptions.sortByTitle() ? SORT_TITLE : SORT_RECENT;
        return builder(ClickName.SET, Screen.COLLECTIONS.get()).object(Optional.of(objectTag.toString())).target(Optional.of(target)).clickCategory(Optional.of(COLLECTION_CATEGORY)).build();
    }

    public static CollectionEvent forRecentlyPlayed(Urn clickObject, Screen screen) {
        return builder(ClickName.ITEM_NAVIGATION, screen.get()).object(Optional.of(clickObject.toString()))
                                                               .source(Optional.of(DiscoverySource.RECENTLY_PLAYED))
                                                               .build();
    }

    private static FilterTag getObjectTag(PlaylistsOptions options) {
        if (options.showLikes() && !options.showPosts()) {
            return LIKED;
        } else if (options.showPosts() && !options.showLikes()) {
            return CREATED;
        } else {
            return ALL;
        }
    }

    private static CollectionEvent.Builder builder(ClickName clickName, String pageName) {
        return new AutoValue_CollectionEvent.Builder().id(defaultId())
                                                      .timestamp(defaultTimestamp())
                                                      .referringEvent(Optional.absent())
                                                      .clickName(clickName)
                                                      .clickCategory(Optional.absent())
                                                      .object(Optional.absent())
                                                      .target(Optional.absent())
                                                      .pageName(pageName)
                                                      .source(Optional.absent());
    }

    @AutoValue.Builder
    public abstract static class Builder {
        public abstract Builder id(String id);

        public abstract Builder timestamp(long timestamp);

        public abstract Builder referringEvent(Optional<ReferringEvent> referringEvent);

        public abstract Builder clickName(ClickName clickName);

        public abstract Builder clickCategory(Optional<String> clickCategory);

        public abstract Builder object(Optional<String> object);

        public abstract Builder target(Optional<Target> target);

        public abstract Builder pageName(String pageName);

        public abstract Builder source(Optional<DiscoverySource> source);

        public abstract CollectionEvent build();
    }
}
