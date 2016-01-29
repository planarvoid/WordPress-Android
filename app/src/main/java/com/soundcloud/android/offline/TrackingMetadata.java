package com.soundcloud.android.offline;

import com.soundcloud.android.model.Urn;
import com.soundcloud.java.objects.MoreObjects;

public class TrackingMetadata {

    private final Urn creatorUrn;
    private boolean fromLikes;
    private boolean fromPlaylists;

    public TrackingMetadata(Urn creatorUrn, boolean fromLikes, boolean fromPlaylists) {
        this.creatorUrn = creatorUrn;
        this.fromLikes = fromLikes;
        this.fromPlaylists = fromPlaylists;
    }

    public void update(TrackingMetadata metadata){
        this.fromLikes = fromLikes || metadata.fromLikes;
        this.fromPlaylists = fromPlaylists || metadata.fromPlaylists;
    }

    public Urn getCreatorUrn() {
        return creatorUrn;
    }

    public boolean isFromLikes() {
        return fromLikes;
    }

    public boolean isFromPlaylists() {
        return fromPlaylists;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TrackingMetadata that = (TrackingMetadata) o;
        return MoreObjects.equal(fromLikes, that.fromLikes) &&
                MoreObjects.equal(fromPlaylists, that.fromPlaylists) &&
                MoreObjects.equal(creatorUrn, that.creatorUrn);
    }

    @Override
    public int hashCode() {
        return MoreObjects.hashCode(creatorUrn, fromLikes, fromPlaylists);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("creatorUrn", creatorUrn)
                .add("fromLikes", fromLikes)
                .add("fromPlaylists", fromPlaylists)
                .toString();
    }
}
