package com.soundcloud.android.image;

import com.google.auto.value.AutoValue;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.tracks.TrackItem;
import com.soundcloud.java.optional.Optional;

@AutoValue
public abstract class SimpleImageResource implements ImageResource {

    public static SimpleImageResource create(Urn urn, Optional<String> avatarUrlTemplate) {
        return new AutoValue_SimpleImageResource(urn, avatarUrlTemplate);
    }

    public static SimpleImageResource create(TrackItem trackItem) {
        return new AutoValue_SimpleImageResource(trackItem.getUrn(),
                                                 trackItem.getImageUrlTemplate());
    }

    @Override
    public abstract Urn getUrn();

    @Override
    public abstract Optional<String> getImageUrlTemplate();
}
