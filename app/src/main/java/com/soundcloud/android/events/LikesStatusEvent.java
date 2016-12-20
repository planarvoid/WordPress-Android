package com.soundcloud.android.events;

import com.google.auto.value.AutoValue;
import com.soundcloud.android.model.Urn;
import com.soundcloud.java.optional.Optional;

import java.util.Collections;
import java.util.Map;

@AutoValue
public abstract class LikesStatusEvent {

    public abstract Map<Urn, LikeStatus> likes();

    public static LikesStatusEvent create(Urn urn, boolean liked, int likesCount) {
        return new AutoValue_LikesStatusEvent(Collections.singletonMap(urn, LikeStatus.create(urn, liked, likesCount)));
    }

    public static LikesStatusEvent createFromSync(Map<Urn, LikeStatus> likes) {
        return new AutoValue_LikesStatusEvent(likes);
    }

    public boolean containsTrackChange() {
        for (Urn urn : likes().keySet()) {
            if (urn.isTrack()) return true;
        }
        return false;
    }

    public boolean containsPlaylistChange() {
        for (Urn urn : likes().keySet()) {
            if (urn.isPlaylist()) return true;
        }
        return false;
    }

    public boolean containsLikedPlaylist() {
        for (LikeStatus like : likes().values()) {
            if (like.urn().isPlaylist()) return like.isUserLike();
        }
        return false;
    }

    public boolean containsUnlikedPlaylist() {
        for (LikeStatus like : likes().values()) {
            if (like.urn().isPlaylist()) return !like.isUserLike();
        }
        return false;
    }

    public Optional<LikeStatus> likeStatusForUrn(Urn urn) {
        if (likes().containsKey(urn)) {
            return Optional.of(likes().get(urn));
        }
        return Optional.absent();
    }

    @AutoValue
    public abstract static class LikeStatus {
        public abstract Urn urn();

        public abstract boolean isUserLike();

        public abstract Optional<Integer> likeCount();

        public static LikeStatus create(Urn urn, boolean isUserLike) {
            return new AutoValue_LikesStatusEvent_LikeStatus(urn, isUserLike, Optional.absent());
        }

        public static LikeStatus create(Urn urn, boolean isUserLike, int likesCount) {
            return new AutoValue_LikesStatusEvent_LikeStatus(urn, isUserLike, Optional.of(likesCount));
        }
    }
}
