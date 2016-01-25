package com.soundcloud.android.collection;

import com.soundcloud.android.model.Urn;
import com.soundcloud.android.offline.OfflineProperty;
import com.soundcloud.android.offline.OfflineState;
import com.soundcloud.java.collections.PropertySet;

import java.util.List;

final class LikesItem {

    private final List<Urn> likes;
    private final PropertySet properties;

    static LikesItem fromUrns(List<Urn> likes) {
        return new LikesItem(likes, PropertySet.create());
    }

    LikesItem(List<Urn> likes, PropertySet properties) {
        this.likes = likes;
        this.properties = properties;
    }

    public void update(PropertySet properties) {
        this.properties.update(properties);
    }

    public List<Urn> getUrns() {
        return likes;
    }

    public OfflineState getDownloadState() {
        return properties.getOrElse(OfflineProperty.OFFLINE_STATE, OfflineState.NOT_OFFLINE);
    }

}
