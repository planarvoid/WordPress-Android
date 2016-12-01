package com.soundcloud.android.events;

import com.soundcloud.android.collection.playlists.PlaylistsOptions;
import com.soundcloud.android.main.Screen;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playback.DiscoverySource;

public final class CollectionEvent extends LegacyTrackingEvent {

    public static final String KIND_SET = "filter_set";
    public static final String KIND_CLEAR = "filter_clear";
    public static final String KIND_RECENTLY_PLAYED_NAVIGATION = "recently_played";

    public static final String KEY_OBJECT = "click_object";
    public static final String KEY_TARGET = "click_target";
    public static final String KEY_PAGE_NAME = "page_name";
    public static final String KEY_SOURCE = "source";

    public static final String CLICK_NAME_ITEM_NAVIGATION = "item_navigation";

    public static final String FILTER_ALL = "filter:all";
    public static final String FILTER_CREATED = "filter:created";
    public static final String FILTER_LIKED = "filter:liked";

    public static final String SORT_TITLE = "sort:title";
    public static final String SORT_RECENT = "sort:recent";

    private CollectionEvent(String kind) {
        super(kind);
    }

    public static CollectionEvent forClearFilter() {
        return new CollectionEvent(KIND_CLEAR);
    }

    public static CollectionEvent forFilter(PlaylistsOptions currentOptions) {
        CollectionEvent base = new CollectionEvent(KIND_SET);
        base.put(KEY_OBJECT, getObjectTag(currentOptions));
        base.put(KEY_TARGET, currentOptions.sortByTitle() ? SORT_TITLE : SORT_RECENT);
        return base;
    }

    public static CollectionEvent forRecentlyPlayed(Urn clickObject, Screen screen) {
        CollectionEvent base = new CollectionEvent(KIND_RECENTLY_PLAYED_NAVIGATION);
        base.put(KEY_OBJECT, clickObject.toString());
        base.put(KEY_PAGE_NAME, screen.get());
        base.put(KEY_SOURCE, DiscoverySource.RECENTLY_PLAYED.value());
        return base;
    }

    private static String getObjectTag(PlaylistsOptions options) {
        if (options.showLikes() && !options.showPosts()) {
            return FILTER_LIKED;
        } else if (options.showPosts() && !options.showLikes()) {
            return FILTER_CREATED;
        } else {
            return FILTER_ALL;
        }
    }
}
