package com.soundcloud.android.sync.likes;

import com.google.auto.value.AutoValue;
import com.soundcloud.android.model.Urn;

import java.util.Date;

@AutoValue
public abstract class DatabaseLikeRecord implements LikeRecord {

    public static DatabaseLikeRecord create(Urn targetUrn, Date createdAt) {
        return new AutoValue_DatabaseLikeRecord(targetUrn, createdAt);
    }
}
