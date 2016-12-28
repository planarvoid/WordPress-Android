package com.soundcloud.android.stream;

import com.google.auto.value.AutoValue;
import com.soundcloud.android.presentation.PlayableItem;
import com.soundcloud.java.collections.PropertySet;

import java.util.Date;

@AutoValue
abstract class StreamPlayable {
    static StreamPlayable createFromPropertySet(Date createdAt, PropertySet propertySet) {
        return new AutoValue_StreamPlayable(createdAt, PlayableItem.from(propertySet));
    }
    static StreamPlayable createFromPlayable(Date createdAt, PlayableItem playableItem) {
        return new AutoValue_StreamPlayable(createdAt, playableItem);
    }
    public abstract Date createdAt();
    public abstract PlayableItem playableItem();
}
