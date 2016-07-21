package com.soundcloud.android.sync.charts;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;
import com.soundcloud.android.image.ImageResource;
import com.soundcloud.android.model.Urn;
import com.soundcloud.java.optional.Optional;

import android.support.annotation.Nullable;

@AutoValue
public abstract class ApiImageResource implements ImageResource {
    @JsonCreator
    public static ApiImageResource create(@JsonProperty("urn") Urn urn,
                                          @JsonProperty("artwork_url_template") @Nullable String artworkUrlTemplate) {
        return new AutoValue_ApiImageResource(urn, Optional.fromNullable(artworkUrlTemplate));
    }
}
