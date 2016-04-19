package com.soundcloud.android.image;

import com.google.auto.value.AutoValue;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.tracks.TrackProperty;
import com.soundcloud.java.collections.PropertySet;
import com.soundcloud.java.optional.Optional;

@AutoValue
public abstract class SimpleImageResource implements ImageResource {

    public static SimpleImageResource create(PropertySet trackProperties) {
        return new AutoValue_SimpleImageResource(trackProperties.get(TrackProperty.URN),
                trackProperties.get(TrackProperty.IMAGE_URL_TEMPLATE));
    }

    @Override
    public abstract Urn getUrn();

    @Override
    public abstract Optional<String> getImageUrlTemplate();
}
