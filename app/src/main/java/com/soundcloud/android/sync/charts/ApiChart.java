package com.soundcloud.android.sync.charts;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.soundcloud.android.api.model.ApiTrack;
import com.soundcloud.android.api.model.ChartCategory;
import com.soundcloud.android.api.model.ChartType;
import com.soundcloud.android.api.model.ModelCollection;
import com.soundcloud.android.model.Urn;

public class ApiChart {
    private final String displayName;
    private final Urn genre;
    private final ChartType type;
    private final ChartCategory category;
    private final ModelCollection<ApiTrack> tracks;

    public ApiChart(@JsonProperty("displayName") String displayName,
                    @JsonProperty("genre") Urn genre,
                    @JsonProperty("type") ChartType type,
                    @JsonProperty("category") ChartCategory category,
                    @JsonProperty("tracks") ModelCollection<ApiTrack> tracks) {
        this.displayName = displayName;
        this.genre = genre;
        this.type = type;
        this.category = category;
        this.tracks = tracks;
    }

    public String getDisplayName() {
        return displayName;
    }

    public Urn getGenre() {
        return genre;
    }

    public ChartType getType() {
        return type;
    }

    public ChartCategory getCategory() {
        return category;
    }

    public ModelCollection<ApiTrack> getTracks() {
        return tracks;
    }
}
