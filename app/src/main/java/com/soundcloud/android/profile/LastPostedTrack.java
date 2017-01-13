package com.soundcloud.android.profile;

import com.google.auto.value.AutoValue;
import com.soundcloud.android.model.Urn;

import java.util.Date;

@AutoValue
public abstract class LastPostedTrack {
    public abstract Urn urn();

    public abstract Date createdAt();

    public abstract String permalinkUrl();

    public static LastPostedTrack create(Urn urn, Date createdAt, String permalinkUrl) {
        return new AutoValue_LastPostedTrack(urn, createdAt, permalinkUrl);
    }
}
