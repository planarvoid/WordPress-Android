package com.soundcloud.android.search.suggestions;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.soundcloud.android.model.Urn;

import java.util.Collections;
import java.util.List;

class ApiSearchSuggestions extends SearchSuggestions<ApiSearchSuggestion> {

    private static final ApiSearchSuggestions EMPTY = new ApiSearchSuggestions(Collections.emptyList(),
                                                                               Urn.NOT_SET);

    public static ApiSearchSuggestions empty() {
        return EMPTY;
    }

    @JsonCreator
    protected ApiSearchSuggestions(@JsonProperty("collection") List<ApiSearchSuggestion> collection,
                                   @JsonProperty("query_urn") Urn queryUrn) {
        super(collection, queryUrn);
    }
}
