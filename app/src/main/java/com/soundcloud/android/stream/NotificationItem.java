package com.soundcloud.android.stream;

import static com.soundcloud.android.stream.StreamItem.Kind.NOTIFICATION;

import com.soundcloud.android.model.Urn;
import com.soundcloud.android.presentation.ListItem;
import com.soundcloud.java.collections.PropertySet;

public class NotificationItem implements StreamItem {

    @Override
    public ListItem update(PropertySet sourceSet) {
        return this;
    }

    @Override
    public Urn getEntityUrn() {
        return Urn.NOT_SET;
    }

    public int getLayout() {
        throw new IllegalArgumentException("layout not set");
    }

    @Override
    public Kind getKind() {
        return NOTIFICATION;
    }
}
