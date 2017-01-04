package com.soundcloud.android.associations;

import com.soundcloud.android.model.Urn;
import com.soundcloud.java.objects.MoreObjects;

import java.util.Set;

public class RepostStatuses {

    private final Set<Urn> reposts;

    public RepostStatuses(Set<Urn> reposts) {
        this.reposts = reposts;
    }

    public boolean isReposted(Urn urn) {
        return reposts.contains(urn);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof RepostStatuses)) return false;
        RepostStatuses that = (RepostStatuses) o;
        return MoreObjects.equal(reposts, that.reposts);
    }

    @Override
    public int hashCode() {
        return MoreObjects.hashCode(reposts);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this).addValue(reposts).toString();
    }
}
