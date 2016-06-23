package com.soundcloud.android.sync.charts;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.soundcloud.android.api.model.ApiTrack;
import com.soundcloud.android.api.model.ChartCategory;
import com.soundcloud.android.api.model.ChartType;
import com.soundcloud.android.api.model.ModelCollection;
import com.soundcloud.android.model.Urn;

import java.util.Collections;
import java.util.List;

public class ApiChart {

    private final String displayName;
    private final Urn genre;
    private final ChartType type;
    private final ChartCategory category;
    private final List<ApiTrack> tracks;

    public ApiChart(@JsonProperty("displayName") String displayName,
                    @JsonProperty("genre") final Urn genre,
                    @JsonProperty("type") ChartType type,
                    @JsonProperty("category") ChartCategory category,
                    @JsonProperty("tracks") ModelCollection<ApiTrack> tracks) {
        this.displayName = displayName;
        this.genre = genre;
        this.type = type;
        this.category = category;
        this.tracks = Collections.unmodifiableList(tracks.getCollection());
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

    public List<ApiTrack> tracks() {
        return tracks;
    }
}
