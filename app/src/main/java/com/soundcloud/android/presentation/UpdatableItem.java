package com.soundcloud.android.presentation;

import com.soundcloud.java.collections.PropertySet;

public interface UpdatableItem {
    /**
     * Update this item's internal state from the given source set.
     *
     * @param sourceSet the set to updated from
     * @return this item
     */
    ListItem updated(PropertySet sourceSet);
}
