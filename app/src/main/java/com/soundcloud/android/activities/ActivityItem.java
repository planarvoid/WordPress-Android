package com.soundcloud.android.activities;

import com.soundcloud.android.model.Urn;
import com.soundcloud.android.presentation.ListItem;
import com.soundcloud.java.collections.PropertySet;
import com.soundcloud.java.strings.Strings;

import java.util.Date;

class ActivityItem implements ListItem {

    private final PropertySet sourceSet;

    ActivityItem(PropertySet sourceSet) {
        this.sourceSet = sourceSet;
    }

    @Override
    public ActivityItem update(PropertySet updatedProperties) {
        this.sourceSet.update(updatedProperties);
        return this;
    }

    ActivityKind getKind() {
        return sourceSet.get(ActivityProperty.KIND);
    }

    String getUserName() {
        return sourceSet.get(ActivityProperty.USER_NAME);
    }

    Date getDate() {
        return sourceSet.get(ActivityProperty.DATE);
    }

    String getPlayableTitle() {
        return sourceSet.getOrElse(ActivityProperty.PLAYABLE_TITLE, Strings.EMPTY);
    }

    @Override
    public Urn getEntityUrn() {
        return sourceSet.get(ActivityProperty.USER_URN);
    }
}
