package com.soundcloud.android.profile;

import com.google.auto.value.AutoValue;
import com.soundcloud.android.api.model.PagedRemoteCollection;

@AutoValue
abstract class UserSoundsBucket {
    public static UserSoundsBucket create(String title, final int collectionType, PagedRemoteCollection pagedRemoteCollection) {
        return new AutoValue_UserSoundsBucket(title, collectionType, pagedRemoteCollection);
    }

    abstract String getTitle();

    abstract int getCollectionType();

    abstract PagedRemoteCollection getPagedRemoteCollection();
}
