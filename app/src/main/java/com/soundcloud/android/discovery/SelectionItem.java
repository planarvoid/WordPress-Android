package com.soundcloud.android.discovery;

import com.google.auto.value.AutoValue;
import com.soundcloud.android.image.ImageStyle;
import com.soundcloud.android.model.Urn;
import com.soundcloud.java.optional.Optional;

@AutoValue
abstract class SelectionItem {
    abstract Optional<Urn> urn();

    abstract Urn selectionUrn();

    abstract Optional<String> artworkUrlTemplate();

    abstract Optional<ImageStyle> artworkStyle();

    abstract Optional<Integer> count();

    abstract Optional<String> shortTitle();

    abstract Optional<String> shortSubtitle();

    abstract Optional<String> appLink();

    abstract Optional<String> webLink();

    Optional<String> link() {
        return appLink().or(webLink());
    }

    static SelectionItem create(Optional<Urn> urn,
                                Urn selectionUrn,
                                Optional<String> artworkUrlTemplate,
                                Optional<ImageStyle> artworkStyle,
                                Optional<Integer> count,
                                Optional<String> shortTitle,
                                Optional<String> shortSubtitle,
                                Optional<String> appLink,
                                Optional<String> webLink) {
        return new AutoValue_SelectionItem(urn, selectionUrn, artworkUrlTemplate, artworkStyle, count, shortTitle, shortSubtitle, appLink, webLink);
    }
}
