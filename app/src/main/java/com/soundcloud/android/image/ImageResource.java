package com.soundcloud.android.image;

import com.soundcloud.android.model.Urn;
import com.soundcloud.java.optional.Optional;

public interface ImageResource {

    Urn getUrn();

    Optional<String> getArtworkUrlTemplate();

}
