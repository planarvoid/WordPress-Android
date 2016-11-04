package com.soundcloud.android.profile;

import com.soundcloud.android.model.Urn;
import com.soundcloud.java.optional.Optional;

import android.support.v7.graphics.Palette;

public interface UserImageSource {
    void setPalette(Optional<Palette> paletteOptional);

    void setShouldDefaultToPalette(boolean shouldDefault);

    Optional<Palette> getPalette();

    boolean shouldDefaultToPalette();

    Urn getCreatorUrn();

    Optional<String> getAvatarUrl();

    Optional<String> getVisualUrl();
}
