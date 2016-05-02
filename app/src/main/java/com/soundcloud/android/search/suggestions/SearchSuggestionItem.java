package com.soundcloud.android.search.suggestions;

import com.soundcloud.android.image.ImageResource;
import com.soundcloud.android.model.EntityProperty;
import com.soundcloud.android.model.Urn;
import com.soundcloud.java.collections.PropertySet;
import com.soundcloud.java.optional.Optional;

class SearchSuggestionItem extends SuggestionItem implements ImageResource {

    private final PropertySet source;

    SearchSuggestionItem(Kind kind, PropertySet source, String query) {
        super(kind, query);
        this.source = source;
    }

    @Override
    public Urn getUrn() {
        return source.get(SearchSuggestionProperty.URN);
    }

    String getDisplayedText() {
        return source.get(SearchSuggestionProperty.DISPLAY_TEXT);
    }

    @Override
    public Optional<String> getImageUrlTemplate() {
        return source.getOrElse(EntityProperty.IMAGE_URL_TEMPLATE, Optional.<String>absent());
    }
}
