package com.soundcloud.android.associations;

import com.google.auto.value.AutoValue;
import com.soundcloud.android.model.Urn;

import java.util.Set;

@AutoValue
public abstract class RepostStatuses {

    protected abstract Set<Urn> reposts();

    public static RepostStatuses create(Set<Urn> reposts) {
        return new AutoValue_RepostStatuses(reposts);
    }

    public boolean isReposted(Urn urn) {
        return reposts().contains(urn);
    }
}
