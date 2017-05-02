package com.soundcloud.android.discovery;

import com.google.auto.value.AutoValue;
import com.soundcloud.android.model.Urn;
import com.soundcloud.java.optional.Optional;

@AutoValue
abstract class SelectionItem {
    abstract Urn urn();

    abstract Optional<String> artworkUrlTemplate();

    abstract Optional<Integer> trackCount();

    abstract Optional<String> shortTitle();

    abstract Optional<String> shortSubtitle();

    static SelectionItem create(Urn urn, Optional<String> artworkUrlTemplate, Optional<Integer> trackCount, Optional<String> shortTitle, Optional<String> shortSubtitle) {
        return new AutoValue_SelectionItem(urn, artworkUrlTemplate, trackCount, shortTitle, shortSubtitle);
    }

}
