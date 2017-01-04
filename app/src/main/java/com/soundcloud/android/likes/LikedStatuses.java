package com.soundcloud.android.likes;

import com.soundcloud.android.model.Urn;
import com.soundcloud.java.objects.MoreObjects;

import java.util.Set;

public class LikedStatuses {

    private final Set<Urn> likes;

    public LikedStatuses(Set<Urn> likes) {
        this.likes = likes;
    }

    public boolean isLiked(Urn urn) {
        return likes.contains(urn);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof LikedStatuses)) return false;
        LikedStatuses that = (LikedStatuses) o;
        return MoreObjects.equal(likes, that.likes);
    }

    @Override
    public int hashCode() {
        return MoreObjects.hashCode(likes);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                          .addValue(likes)
                          .toString();
    }
}
