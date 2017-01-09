package com.soundcloud.android.stations;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.soundcloud.android.model.Urn;
import com.soundcloud.java.functions.Function;
import com.soundcloud.java.objects.MoreObjects;
import com.soundcloud.java.optional.Optional;

public final class ApiStationMetadata {

    public final static Function<ApiStationMetadata, Urn> TO_URN = input -> input.getUrn();

    private final Urn urn;
    private final String title;
    private final String permalink;
    private final String type;
    private final Optional<String> artworkUrlTemplate;

    @JsonCreator
    ApiStationMetadata(@JsonProperty("urn") Urn urn,
                       @JsonProperty("title") String title,
                       @JsonProperty("permalink") String permalink,
                       @JsonProperty("type") String type,
                       @JsonProperty("artwork_url_template") String artworkUrlTemplate) {
        this.urn = urn;
        this.title = title;
        this.permalink = permalink;
        this.type = type;
        this.artworkUrlTemplate = Optional.fromNullable(artworkUrlTemplate);
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

    public Optional<String> getArtworkUrlTemplate() {
        return artworkUrlTemplate;
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
