package com.soundcloud.android.discovery.newforyou;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;
import com.soundcloud.android.api.model.ApiTrack;
import com.soundcloud.android.api.model.ModelCollection;

import java.util.Date;

@AutoValue
abstract class ApiNewForYou {

    abstract Date lastUpdate();
    abstract ModelCollection<ApiTrack> tracks();

    @JsonCreator
    public static ApiNewForYou create(@JsonProperty("last_updated") Date lastUpdated, @JsonProperty("tracks") ModelCollection<ApiTrack> tracks) {
        return new AutoValue_ApiNewForYou(lastUpdated, tracks);
    }
}
