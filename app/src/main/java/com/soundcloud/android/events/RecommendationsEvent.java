package com.soundcloud.android.events;

import com.soundcloud.android.main.Screen;
import com.soundcloud.android.model.Urn;
import org.jetbrains.annotations.NotNull;

public class RecommendationsEvent extends TrackingEvent {

    public static final String KIND_SEED_TRACK_CLICK = "seed_track_click";
    public static final String KIND_RECOMMENDED_TRACK_CLICK = "recommended_track_click";

    public static final String KEY_PAGE_NAME = "page_name";
    public static final String KEY_CLICK_NAME = "click_name";
    public static final String KEY_CLICK_OBJECT = "click_object";
    public static final String KEY_QUERY_URN = "query_urn";

    public static final String CLICK_NAME_ITEM_NAVIGATION = "item_navigation";

    private RecommendationsEvent(@NotNull String kind, Screen trackingScreen, Urn trackUrn, Urn queryUrn) {
        super(kind, System.currentTimeMillis());
        put(KEY_PAGE_NAME, trackingScreen.get());
        put(KEY_CLICK_NAME, CLICK_NAME_ITEM_NAVIGATION);
        put(KEY_CLICK_OBJECT, trackUrn.toString());
        put(KEY_QUERY_URN, queryUrn.toString());
    }

    public static RecommendationsEvent forSeedTrackClick(Screen trackingScreen, Urn trackUrn, Urn queryUrn) {
        return new RecommendationsEvent(KIND_SEED_TRACK_CLICK, trackingScreen, trackUrn, queryUrn);
    }

    public static RecommendationsEvent forTrackClick(Screen trackingScreen, Urn trackUrn, Urn queryUrn) {
        return new RecommendationsEvent(KIND_RECOMMENDED_TRACK_CLICK, trackingScreen, trackUrn, queryUrn);
    }

    @Override
    public String toString() {
        return String.format("Recommendations Event with type id %s and %s", kind, attributes.toString());
    }
}
