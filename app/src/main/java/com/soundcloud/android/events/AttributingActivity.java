package com.soundcloud.android.events;

import com.google.auto.value.AutoValue;
import com.soundcloud.android.presentation.PlayableItem;
import com.soundcloud.android.presentation.PromotedListItem;

@AutoValue
public abstract class AttributingActivity {
    public static final String POSTED = "posted";
    public static final String REPOSTED = "reposted";
    public static final String PROMOTED = "promoted";

    public static AttributingActivity create(String type, String resource) {
        return new AutoValue_AttributingActivity(type, resource);
    }

    public abstract String getType();

    public abstract String getResource();

    public static String typeFromPlayableItem(PlayableItem playableItem) {
        if (playableItem instanceof PromotedListItem) {
            return AttributingActivity.PROMOTED;
        } else if (playableItem.getReposter().isPresent()) {
            return AttributingActivity.REPOSTED;
        } else {
            return AttributingActivity.POSTED;
        }
    }
}
