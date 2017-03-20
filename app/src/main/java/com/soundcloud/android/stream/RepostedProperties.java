package com.soundcloud.android.stream;

import com.google.auto.value.AutoValue;
import com.soundcloud.android.model.Urn;
import com.soundcloud.java.optional.Optional;

@AutoValue
public abstract class RepostedProperties {
    public abstract String reposter();

    public abstract Urn reposterUrn();

    public static RepostedProperties create(String reposter, Urn reposterUrn) {
        return new AutoValue_RepostedProperties(reposter, reposterUrn);
    }
}
