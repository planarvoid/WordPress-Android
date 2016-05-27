package com.soundcloud.android.discovery;

import com.google.auto.value.AutoValue;
import com.soundcloud.android.image.ImageResource;
import com.soundcloud.android.model.Urn;
import com.soundcloud.java.optional.Optional;

@AutoValue
public abstract class ChartTrack implements ImageResource {
    public static ChartTrack create(Urn urn, Optional<String> imageUrlTemplate) {
        return new AutoValue_ChartTrack(urn, imageUrlTemplate);
    }
}
