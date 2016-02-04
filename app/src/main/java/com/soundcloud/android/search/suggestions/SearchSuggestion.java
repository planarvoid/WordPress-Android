package com.soundcloud.android.search.suggestions;

import com.soundcloud.android.model.Urn;
import com.soundcloud.java.objects.MoreObjects;

import java.util.List;
import java.util.Map;

public abstract class SearchSuggestion {
    abstract Urn getUrn();

    abstract String getQuery();

    abstract List<Map<String, Integer>> getHighlights();

    abstract boolean isRemote();

    @Override
    public final boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o != null && o instanceof SearchSuggestion) {
            SearchSuggestion that = (SearchSuggestion) o;
            return MoreObjects.equal(this.getUrn(), that.getUrn());
        } else {
            return false;
        }
    }

    @Override
    public final int hashCode() {
        return MoreObjects.hashCode(getUrn());
    }
}
