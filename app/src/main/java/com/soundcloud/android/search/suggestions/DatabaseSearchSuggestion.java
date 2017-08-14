package com.soundcloud.android.search.suggestions;

import com.google.auto.value.AutoValue;
import com.soundcloud.android.model.Urn;
import com.soundcloud.java.optional.Optional;

@AutoValue
abstract class DatabaseSearchSuggestion extends SearchSuggestion {

    public abstract Optional<String> title();

    public static DatabaseSearchSuggestion create(Urn urn, String query, Optional<String> imageUrlTemplate, Optional<String> title) {
        return new AutoValue_DatabaseSearchSuggestion(urn, query, Optional.absent(), false, imageUrlTemplate, title);
    }
}
