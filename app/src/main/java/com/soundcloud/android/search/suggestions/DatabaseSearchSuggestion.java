package com.soundcloud.android.search.suggestions;

import com.google.auto.value.AutoValue;
import com.soundcloud.android.model.Urn;
import com.soundcloud.java.optional.Optional;

@AutoValue
abstract class DatabaseSearchSuggestion extends SearchSuggestion {

    public enum Kind {
        Like, Following, Post, LikeByUsername
    }

    public abstract Kind kind();

    public static DatabaseSearchSuggestion create(Urn urn, String query, Optional<String> imageUrlTemplate, Kind kind) {
        return new AutoValue_DatabaseSearchSuggestion(urn, query, Optional.absent(), false, imageUrlTemplate, kind);
    }
}
