package com.soundcloud.android.discovery;

import com.google.auto.value.AutoValue;
import com.soundcloud.android.image.ImageResource;
import com.soundcloud.android.model.Urn;
import com.soundcloud.java.optional.Optional;

@AutoValue
abstract class SelectionItem implements ImageResource {
    abstract Urn urn();

    abstract Optional<String> artworkUrlTemplate();

    abstract Optional<Integer> count();

    abstract Optional<String> shortTitle();

    abstract Optional<String> shortSubtitle();

    static SelectionItem create(Urn urn, Optional<String> artworkUrlTemplate, Optional<Integer> count, Optional<String> shortTitle, Optional<String> shortSubtitle) {
        return new AutoValue_SelectionItem(urn, artworkUrlTemplate, count, shortTitle, shortSubtitle);
    }

    @Override
    public Urn getUrn() {
        return urn();
    }

    @Override
    public Optional<String> getImageUrlTemplate() {
        return artworkUrlTemplate();
    }

}
