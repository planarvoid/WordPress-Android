package com.soundcloud.android.sync.posts;

import com.google.auto.value.AutoValue;
import com.soundcloud.android.model.Urn;

import java.util.Date;

@AutoValue
public abstract class DatabasePostRecord implements PostRecord {
    public static DatabasePostRecord createRepost(Urn targetUrn, Date createdAt) {
        return new AutoValue_DatabasePostRecord(targetUrn, createdAt, true);
    }
    public static DatabasePostRecord createPost(Urn targetUrn, Date createdAt) {
        return new AutoValue_DatabasePostRecord(targetUrn, createdAt, false);
    }
}
