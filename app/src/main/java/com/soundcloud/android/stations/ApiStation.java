package com.soundcloud.android.stations;

import static com.soundcloud.java.collections.Lists.transform;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.soundcloud.android.Consts;
import com.soundcloud.android.api.model.ApiTrack;
import com.soundcloud.android.api.model.ModelCollection;
import com.soundcloud.android.api.model.StationRecord;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.tracks.TrackRecord;
import com.soundcloud.java.functions.Function;
import com.soundcloud.java.objects.MoreObjects;

import android.support.annotation.VisibleForTesting;

import java.util.List;

public final class ApiStation implements StationRecord {

    private static final Function<TrackRecord, Urn> TO_URN = new Function<TrackRecord, Urn>() {
        @Override
        public Urn apply(TrackRecord track) {
            return track.getUrn();
        }
    };

    private final ApiStationMetadata metadata;
    private final List<? extends TrackRecord> tracks;

    @JsonCreator
    public ApiStation(@JsonProperty("station") ApiStationMetadata metadata, @JsonProperty("tracks") ModelCollection<ApiTrack> tracks) {
        this.metadata = metadata;
        this.tracks = tracks.getCollection();
    }

    @VisibleForTesting
    public ApiStationMetadata getMetadata() {
        return metadata;
    }

    @Override
    public List<Urn> getTracks() {
        return transform(tracks, TO_URN);
    }

    public List<? extends TrackRecord> getTrackRecords() {
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

    public String getPermalink() {
        return metadata.getPermalink();
    }

    @Override
    public int getPreviousPosition() {
        return Consts.NOT_SET;
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
