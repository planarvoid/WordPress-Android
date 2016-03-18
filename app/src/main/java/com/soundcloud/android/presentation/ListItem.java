package com.soundcloud.android.presentation;

import com.soundcloud.android.image.ImageResource;
import com.soundcloud.android.model.Entity;
import com.soundcloud.java.collections.PropertySet;

/**
 * The base interface for all presentation models that act as items in Android ListViews.
 * <p/>
 * ListItems should update their internal state from system events via {@link #update(com.soundcloud.java.collections.PropertySet)}
 * and as such expose their public properties in terms of a backing property set.
 */
public interface ListItem extends Entity, ImageResource {

    /**
     * Update this item's internal state from the given source set.
     *
     * @param sourceSet the set to update from
     * @return this item
     */
    ListItem update(PropertySet sourceSet);

}
