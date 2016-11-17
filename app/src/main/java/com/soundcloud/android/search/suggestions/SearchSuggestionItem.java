package com.soundcloud.android.search.suggestions;

import com.google.auto.value.AutoValue;
import com.soundcloud.android.image.ImageResource;
import com.soundcloud.android.model.EntityProperty;
import com.soundcloud.android.model.Urn;
import com.soundcloud.java.collections.PropertySet;
import com.soundcloud.java.optional.Optional;

@AutoValue
abstract class SearchSuggestionItem extends SuggestionItem implements ImageResource {

    abstract PropertySet source();

    @Override
    public Urn getUrn() {
        return source().get(SearchSuggestionProperty.URN);
    }

    String getDisplayedText() {
        return source().get(SearchSuggestionProperty.DISPLAY_TEXT);
    }

    @Override
    public Optional<String> getImageUrlTemplate() {
        return source().getOrElse(EntityProperty.IMAGE_URL_TEMPLATE, Optional.<String>absent());
    }

    Optional<SuggestionHighlight> getSuggestionHighlight() {
        return source().get(SearchSuggestionProperty.HIGHLIGHT);
    }
}
