package com.soundcloud.android.api.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.soundcloud.android.tracks.TrackRecord;
import com.soundcloud.java.objects.MoreObjects;

public final class ApiStation implements StationRecord {

    private final ApiStationInfo info;
    private final ModelCollection<? extends TrackRecord> tracks;

    @JsonCreator
    public ApiStation(@JsonProperty("info") ApiStationInfo info, @JsonProperty("tracks") ModelCollection<ApiTrack> tracks) {
        this.info = info;
        this.tracks = tracks;
    }

    @Override
    public ApiStationInfo getInfo() {
        return info;
    }

    @Override
    public ModelCollection<? extends TrackRecord> getTracks() {
        return tracks;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        ApiStation that = (ApiStation) o;
        return MoreObjects.equal(info, that.info) &&
                MoreObjects.equal(tracks, that.tracks);
    }

    @Override
    public int hashCode() {
        return MoreObjects.hashCode(info, tracks);
    }
}
