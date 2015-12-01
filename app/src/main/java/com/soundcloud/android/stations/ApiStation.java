package com.soundcloud.android.stations;

import static com.soundcloud.java.collections.Lists.transform;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.soundcloud.android.api.model.ApiTrack;
import com.soundcloud.android.api.model.ModelCollection;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.tracks.TrackRecord;
import com.soundcloud.java.functions.Function;
import com.soundcloud.java.objects.MoreObjects;

import android.support.annotation.VisibleForTesting;

import java.util.List;

public final class ApiStation implements StationRecord {

    private final Function<TrackRecord, StationTrack> TO_STATION_TRACK = new Function<TrackRecord, StationTrack>() {
        @Override
        public StationTrack apply(TrackRecord track) {
            return StationTrack.create(track.getUrn(), queryUrn);
        }
    };

    private final ApiStationMetadata metadata;
    private final List<? extends TrackRecord> tracks;
    private final Urn queryUrn;

    @JsonCreator
    public ApiStation(@JsonProperty("station") ApiStationMetadata metadata,
                      @JsonProperty("tracks") ModelCollection<ApiTrack> tracks) {
        this.metadata = metadata;
        this.tracks = tracks.getCollection();
        this.queryUrn = tracks.getQueryUrn().or(Urn.NOT_SET);
    }

    @VisibleForTesting
    public ApiStationMetadata getMetadata() {
        return metadata;
    }

    @Override
    public List<StationTrack> getTracks() {
        return transform(tracks, TO_STATION_TRACK);
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
        return Stations.NEVER_PLAYED;
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
