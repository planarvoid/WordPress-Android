package com.soundcloud.android.search.suggestions;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;

@AutoValue
abstract class Autocompletion {
    abstract String query();
    abstract String output();

    @JsonCreator
    public static Autocompletion create(@JsonProperty("query") String query, @JsonProperty("output") String output) {
        return new AutoValue_Autocompletion(query, output);
    }
}
