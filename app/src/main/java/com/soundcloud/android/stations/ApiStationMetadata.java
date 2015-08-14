package com.soundcloud.android.stations;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.soundcloud.android.model.Urn;
import com.soundcloud.java.objects.MoreObjects;

public final class ApiStationMetadata {

    private final Urn urn;
    private final String title;
    private final String permalink;
    private final String type;

    @JsonCreator
    ApiStationMetadata(@JsonProperty("urn") Urn urn,
                       @JsonProperty("title") String title,
                       @JsonProperty("permalink") String permalink,
                       @JsonProperty("type") String type) {
        this.urn = urn;
        this.title = title;
        this.permalink = permalink;
        this.type = type;
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

    public String getPermalink() {
        return permalink;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        ApiStationMetadata that = (ApiStationMetadata) o;
        return MoreObjects.equal(urn, that.urn);
    }

    @Override
    public int hashCode() {
        return urn.hashCode();
    }

}
