package com.soundcloud.android.api.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.soundcloud.android.model.PropertySetSource;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.stations.StationProperty;
import com.soundcloud.java.collections.PropertySet;
import com.soundcloud.java.objects.MoreObjects;

public final class ApiStationInfo implements PropertySetSource {

    private final Urn urn;
    private final String title;
    private final String type;
    private final ApiTrack seedTrack;

    @JsonCreator
    ApiStationInfo(@JsonProperty("urn") Urn urn,
                   @JsonProperty("title") String title,
                   @JsonProperty("type") String type,
                   @JsonProperty("_embedded") RelatedResources relatedResources) {
        this(urn, title, type, relatedResources.track);
    }

    public ApiStationInfo(Urn urn,
                   String title,
                   String type,
                   ApiTrack seedTrack) {
        this.urn = urn;
        this.title = title;
        this.type = type;
        this.seedTrack = seedTrack;
    }

    public Urn getUrn() {
        return urn;
    }

    public long getId() {
        return urn.getNumericId();
    }

    public String getTitle() {
        return title;
    }

    public String getType() {
        return type;
    }

    public ApiTrack getSeedTrack() {
        return seedTrack;
    }

    @Override
    public PropertySet toPropertySet() {
        return PropertySet.from(
                StationProperty.URN.bind(urn),
                StationProperty.TITLE.bind(title),
                StationProperty.TYPE.bind(type),
                StationProperty.SEED_TRACK_ID.bind(seedTrack.getId()),
                // TODO : update this when implementing play queue logic
                StationProperty.LAST_PLAYED_TRACK_POSITION.bind(0)
        );
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        ApiStationInfo that = (ApiStationInfo) o;
        return MoreObjects.equal(urn, that.urn);
    }

    @Override
    public int hashCode() {
        return urn.hashCode();
    }

    private static class RelatedResources {
        private final ApiTrack track;

        @JsonCreator
        RelatedResources(@JsonProperty("track") ApiTrack track) {
            this.track = track;
        }
    }
}
