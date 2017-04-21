package com.soundcloud.android.home;

import com.google.auto.value.AutoValue;
import com.soundcloud.android.model.Urn;
import com.soundcloud.java.optional.Optional;

@AutoValue
abstract class SelectionPlaylist {
    abstract Urn urn();

    abstract Optional<String> artworkUrlTemplate();

    abstract Optional<Integer> trackCount();

    abstract Optional<String> shortTitle();

    abstract Optional<String> shortSubtitle();

    static SelectionPlaylist create(Urn urn, Optional<String> artworkUrlTemplate, Optional<Integer> trackCount, Optional<String> shortTitle, Optional<String> shortSubtitle) {
        return new AutoValue_SelectionPlaylist(urn, artworkUrlTemplate, trackCount, shortTitle, shortSubtitle);
    }

}
