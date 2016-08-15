package com.soundcloud.android.sync.charts;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.soundcloud.android.api.model.ChartCategory;
import com.soundcloud.android.api.model.ChartType;
import com.soundcloud.android.api.model.ModelCollection;
import com.soundcloud.android.image.ImageResource;
import com.soundcloud.android.model.Urn;
import com.soundcloud.java.optional.Optional;

import java.util.Date;

public class ApiChart<T extends ImageResource> {

    private final String displayName;
    private final Urn genre;
    private final ChartType type;
    private final ChartCategory category;
    private final Date lastUpdated;
    private final ModelCollection<T> tracks;

    public ApiChart(@JsonProperty("displayName") String displayName,
                    @JsonProperty("genre") final Urn genre,
                    @JsonProperty("type") ChartType type,
                    @JsonProperty("category") ChartCategory category,
                    @JsonProperty("last_updated") Date lastUpdated,
                    @JsonProperty("tracks") ModelCollection<T> tracks) {
        this.displayName = displayName;
        this.genre = genre;
        this.type = type;
        this.category = category;
        this.tracks = tracks;
        this.lastUpdated = lastUpdated;
    }

    public String displayName() {
        return displayName;
    }

    public Urn genre() {
        return genre;
    }

    public ChartType type() {
        return type;
    }

    public ChartCategory category() {
        return category;
    }

    public Date lastUpdated() {
        return lastUpdated;
    }

    public ModelCollection<T> tracks() {
        return tracks;
    }

    public Optional<Urn> getQueryUrn() {
        return tracks.getQueryUrn();
    }
}
