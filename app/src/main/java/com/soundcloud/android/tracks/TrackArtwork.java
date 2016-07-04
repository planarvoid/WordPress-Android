package com.soundcloud.android.tracks;

import com.google.auto.value.AutoValue;
import com.soundcloud.android.image.ImageResource;
import com.soundcloud.android.model.Urn;
import com.soundcloud.java.optional.Optional;

@AutoValue
public abstract class TrackArtwork implements ImageResource {
    public static TrackArtwork create(Urn urn, Optional<String> imageUrlTemplate) {
        return new AutoValue_TrackArtwork(urn, imageUrlTemplate);
    }
}
