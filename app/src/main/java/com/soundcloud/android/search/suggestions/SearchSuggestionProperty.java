package com.soundcloud.android.search.suggestions;

import com.soundcloud.android.model.Urn;
import com.soundcloud.java.collections.Property;
import com.soundcloud.java.optional.Optional;

class SearchSuggestionProperty {
    public static final Property<Urn> URN = Property.of(SearchSuggestionProperty.class, Urn.class);
    public static final Property<String> DISPLAY_TEXT = Property.of(SearchSuggestionProperty.class, String.class);
    public static final Property<Optional<SuggestionHighlight>> HIGHLIGHT =
            Property.ofOptional(SearchSuggestionProperty.class, SuggestionHighlight.class);
}
