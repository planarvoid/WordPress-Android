package com.soundcloud.android.collection;

import com.google.auto.value.AutoValue;
import com.soundcloud.android.image.ImageResource;
import com.soundcloud.android.model.Urn;
import com.soundcloud.java.optional.Optional;

import android.support.annotation.Nullable;

@AutoValue
abstract class LikedTrackPreview implements ImageResource {

    static LikedTrackPreview create(Urn urn, @Nullable String imageUrl) {
        return new AutoValue_LikedTrackPreview(urn, Optional.fromNullable(imageUrl));
    }

    @Override
    public abstract Urn getUrn();

    @Override
    public abstract Optional<String> getImageUrlTemplate();
}
