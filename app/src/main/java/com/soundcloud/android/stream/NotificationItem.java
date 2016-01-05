package com.soundcloud.android.stream;

import static com.soundcloud.android.stream.StreamItem.Kind.NOTIFICATION;

import com.soundcloud.android.model.Urn;
import com.soundcloud.android.presentation.ListItem;
import com.soundcloud.java.collections.PropertySet;

import java.util.Date;

public class NotificationItem implements StreamItem {

    private final Date CREATED_AT = new Date();

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

    @Override
    public Date getCreatedAt() {
        return CREATED_AT;
    }

    @Override
    public boolean isUpsellable() {
        return false;
    }
}
