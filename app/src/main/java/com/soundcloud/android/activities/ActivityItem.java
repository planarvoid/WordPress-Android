package com.soundcloud.android.activities;

import com.google.auto.value.AutoValue;
import com.soundcloud.android.api.model.Timestamped;
import com.soundcloud.android.model.Urn;
import com.soundcloud.java.collections.PropertySet;
import com.soundcloud.java.optional.Optional;
import com.soundcloud.java.strings.Strings;

@AutoValue
public abstract class ActivityItem implements Timestamped {
    abstract ActivityKind getKind();
    abstract String getUserName();
    abstract String getPlayableTitle();
    abstract Optional<Urn> getCommentedTrackUrn();
    abstract Urn getUrn();

    public static ActivityItem fromPropertySet(PropertySet sourceSet) {
        return new AutoValue_ActivityItem(sourceSet.get(ActivityProperty.DATE),
                                          sourceSet.get(ActivityProperty.KIND),
                                          sourceSet.get(ActivityProperty.USER_NAME),
                                          sourceSet.getOrElse(ActivityProperty.PLAYABLE_TITLE, Strings.EMPTY),
                                          Optional.fromNullable(sourceSet.getOrElseNull(ActivityProperty.COMMENTED_TRACK_URN)),
                                          sourceSet.get(ActivityProperty.USER_URN));
    }
}
