package com.soundcloud.android.events;

import com.google.auto.value.AutoValue;
import com.soundcloud.android.model.Urn;
import com.soundcloud.java.optional.Optional;

import java.util.Collections;
import java.util.Map;

@AutoValue
public abstract class RepostsStatusEvent {

    public abstract Map<Urn, RepostStatus> reposts();

    public static RepostsStatusEvent createReposted(Urn urn) {
        return new AutoValue_RepostsStatusEvent(Collections.singletonMap(urn, RepostStatus.createReposted(urn)));
    }

    public static RepostsStatusEvent createUnposted(Urn urn) {
        return new AutoValue_RepostsStatusEvent(Collections.singletonMap(urn, RepostStatus.createUnposted(urn)));
    }

    public static RepostsStatusEvent createReposted(Urn urn, int repostCount) {
        return new AutoValue_RepostsStatusEvent(Collections.singletonMap(urn, RepostStatus.createReposted(urn, repostCount)));
    }

    public static RepostsStatusEvent create(Map<Urn, RepostStatus> reposts) {
        return new AutoValue_RepostsStatusEvent(reposts);
    }

    public static RepostsStatusEvent create(RepostStatus repost) {
        return new AutoValue_RepostsStatusEvent(Collections.singletonMap(repost.urn(), repost));
    }

    public boolean containsTrackChange() {
        for (Urn urn : reposts().keySet()) {
            if (urn.isTrack()) return true;
        }
        return false;
    }

    public boolean containsPlaylistChange() {
        for (Urn urn : reposts().keySet()) {
            if (urn.isPlaylist()) return true;
        }
        return false;
    }

    public Optional<RepostStatus> repostStatusForUrn(Urn urn) {
        if (reposts().containsKey(urn)) {
            return Optional.of(reposts().get(urn));
        }
        return Optional.absent();
    }

    @AutoValue
    public abstract static class RepostStatus {
        public abstract Urn urn();

        public abstract boolean isReposted();

        public abstract Optional<Integer> repostCount();

        public static RepostStatus createReposted(Urn urn) {
            return new AutoValue_RepostsStatusEvent_RepostStatus(urn, true, Optional.absent());
        }

        public static RepostStatus createUnposted(Urn urn) {
            return new AutoValue_RepostsStatusEvent_RepostStatus(urn, false, Optional.absent());
        }

        public static RepostStatus createReposted(Urn urn, int repostCount) {
            return new AutoValue_RepostsStatusEvent_RepostStatus(urn, true, Optional.of(repostCount));
        }
        public static RepostStatus createUnposted(Urn urn, int repostCount) {
            return new AutoValue_RepostsStatusEvent_RepostStatus(urn, false, Optional.of(repostCount));
        }
    }
}
