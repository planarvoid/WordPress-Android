package com.soundcloud.android.offline;

import com.soundcloud.android.model.Urn;
import com.soundcloud.java.objects.MoreObjects;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class DownloadRequest {
    public final Urn track;
    public final long duration;
    public final String waveformUrl;
    public final List<Urn> inPlaylists;
    public final boolean inLikedTracks;
    public final boolean syncable;

    public static class Builder {
        private final Urn track;
        private final long duration;
        private final String waveformUrl;
        private final boolean syncable;

        private List<Urn> playlists = new ArrayList<>();
        private boolean inLikes = false;

        public Builder(Urn track, long duration, String waveformUrl, boolean syncable) {
            this.track = track;
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
            return new DownloadRequest(track, duration, waveformUrl, syncable, inLikes, playlists);
        }
    }

    public DownloadRequest(Urn track, long duration, String waveformUrl, boolean syncable,
                           boolean inLikedTracks, List<Urn> inPlaylists) {
        this.track = track;
        this.duration = duration;
        this.waveformUrl = waveformUrl;
        this.syncable = syncable;
        this.inPlaylists = inPlaylists;
        this.inLikedTracks = inLikedTracks;
    }

    public DownloadRequest(Urn trackUrn, long duration, String waveformUrl) {
        this(trackUrn, duration, waveformUrl, true, false, Collections.<Urn>emptyList());
    }

    @Override
    public boolean equals(Object o) {

        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        DownloadRequest that = (DownloadRequest) o;

        return MoreObjects.equal(track, that.track)
                && MoreObjects.equal(inLikedTracks, that.inLikedTracks)
                && MoreObjects.equal(inPlaylists, that.inPlaylists);
    }

    @Override
    public int hashCode() {
        return MoreObjects.hashCode(track, inLikedTracks, inPlaylists);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("track", track)
                .add("inLikedTracks", inLikedTracks)
                .add("inPlaylists", inPlaylists)
                .toString();
    }
}
