package com.soundcloud.android.offline;

import com.google.common.base.Objects;
import com.soundcloud.android.model.Urn;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class DownloadRequest {
    public final String fileUrl;
    public final Urn track;
    public final long duration;
    public final List<Urn> inPlaylists;
    public final boolean inLikedTracks;

    public static class Builder {
        private final Urn track;
        private final long duration;
        private final String stream;

        private List<Urn> playlists = new ArrayList<>();
        private boolean inLikes = false;

        public Builder(Urn track, String stream, long duration) {
            this.track = track;
            this.duration = duration;
            this.stream = stream;
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
            return new DownloadRequest(track, stream, duration, inLikes, playlists);
        }
    }

    public DownloadRequest(Urn track, String url, long duration, boolean inLikedTracks, List<Urn> inPlaylists) {
        this.fileUrl = url;
        this.track = track;
        this.duration = duration;
        this.inPlaylists = inPlaylists;
        this.inLikedTracks = inLikedTracks;
    }

    public DownloadRequest(Urn trackUrn, String stream, long duration) {
        this (trackUrn, stream, duration, false, Collections.<Urn>emptyList());
    }

    @Override
    public boolean equals(Object o) {

        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        DownloadRequest that = (DownloadRequest) o;

        return  Objects.equal(track, that.track) &&
                Objects.equal(fileUrl, that.fileUrl) &&
                Objects.equal(inLikedTracks, that.inLikedTracks) &&
                Objects.equal(inPlaylists, that.inPlaylists);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(fileUrl, track, inLikedTracks, inPlaylists);
    }

    @Override
    public String toString() {
        return Objects.toStringHelper(this)
                .add("track", track)
                .add("fileUrl", fileUrl)
                .add("inLikedTracks", inLikedTracks)
                .add("inPlaylists", inPlaylists)
                .toString();
    }
}
