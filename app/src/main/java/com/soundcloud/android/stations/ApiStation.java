package com.soundcloud.android.stations;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.soundcloud.android.api.model.ApiTrack;
import com.soundcloud.android.api.model.ModelCollection;
import com.soundcloud.android.api.model.StationRecord;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.tracks.TrackRecord;
import com.soundcloud.java.objects.MoreObjects;

public final class ApiStation implements StationRecord {

    private final ApiStationMetadata metadata;
    private final ModelCollection<? extends TrackRecord> tracks;

    @JsonCreator
    public ApiStation(@JsonProperty("station") ApiStationMetadata metadata, @JsonProperty("tracks") ModelCollection<ApiTrack> tracks) {
        this.metadata = metadata;
        this.tracks = tracks;
    }

    ApiStationMetadata getMetadata() {
        return metadata;
    }

    @Override
    public ModelCollection<? extends TrackRecord> getTracks() {
        return tracks;
    }

    @Override
    public Urn getUrn() {
        return metadata.getUrn();
    }

    @Override
    public String getType() {
        return metadata.getType();
    }

    @Override
    public String getTitle() {
        return metadata.getTitle();
    }

    @Override
    public String getPermalink() {
        return metadata.getPermalink();
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
        return MoreObjects.equal(metadata, that.metadata) &&
                MoreObjects.equal(tracks, that.tracks);
    }

    @Override
    public int hashCode() {
        return MoreObjects.hashCode(metadata, tracks);
    }
}
