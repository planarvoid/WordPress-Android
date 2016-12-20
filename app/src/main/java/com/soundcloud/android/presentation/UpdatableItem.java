package com.soundcloud.android.presentation;

import com.soundcloud.android.model.Entity;
import com.soundcloud.java.collections.PropertySet;

public interface UpdatableItem extends Entity {
    /**
     * Update this item's internal state from the given source set.
     *
     * @param sourceSet the set to updated from
     * @return this item
     */
    UpdatableItem updated(PropertySet sourceSet);
}
