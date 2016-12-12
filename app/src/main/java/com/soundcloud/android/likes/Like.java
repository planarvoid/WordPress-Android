package com.soundcloud.android.likes;

import com.google.auto.value.AutoValue;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.model.UrnHolder;

import java.util.Date;

@AutoValue
public abstract class Like implements UrnHolder {

    public static Like create(Urn urn, Date likedAt) {
        return new AutoValue_Like(urn, likedAt);
    }

    public abstract Urn urn();

    public abstract Date likedAt();
}
