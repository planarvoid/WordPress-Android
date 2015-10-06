package com.soundcloud.android.offline;

import static com.soundcloud.java.collections.Lists.newArrayList;

import com.google.auto.value.AutoValue;
import com.soundcloud.android.model.Urn;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@AutoValue
public abstract class DownloadRequest {

    public static DownloadRequest create(OfflineTrackContext offlineTrackContext, long duration,
                                         String waveformUrl, boolean syncable) {
        return new AutoValue_DownloadRequest(offlineTrackContext, duration, waveformUrl, syncable);
    }

    public abstract OfflineTrackContext getTrackContext();

    public abstract long getDuration();

    public abstract String getWaveformUrl();

    public abstract boolean isSyncable();

    public Urn getTrack() {
        return getTrackContext().getTrack();
    }

    public List<Urn> getPlaylists() {
        return getTrackContext().inPlaylists();
    }

    public boolean isLiked() {
        return getTrackContext().inLikes();
    }

    static class Builder {
        private final Urn track;
        private final Urn creator;
        private final long duration;
        private final String waveformUrl;
        private final boolean syncable;

        private Set<Urn> playlists = new HashSet<>();
        private boolean inLikes = false;

        public Builder(Urn track, Urn creator, long duration, String waveformUrl, boolean syncable) {
            this.track = track;
            this.creator = creator;
            this.duration = duration;
            this.waveformUrl = waveformUrl;
            this.syncable = syncable;
        }

        public Builder addToLikes(boolean inLikes) {
            if (!this.inLikes) {
                this.inLikes = inLikes;
            }
            return this;
        }

        public Builder addToPlaylist(Urn playlist) {
            if (playlist != Urn.NOT_SET) {
                playlists.add(playlist);
            }
            return this;
        }

        public DownloadRequest build() {
            final OfflineTrackContext trackContext = OfflineTrackContext.create(track, creator, newArrayList(playlists), inLikes);
            return DownloadRequest.create(trackContext, duration, waveformUrl, syncable);
        }
    }
}
