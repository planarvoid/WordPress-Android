package com.soundcloud.android.search.suggestions;

import com.google.auto.value.AutoValue;
import com.soundcloud.android.model.Urn;
import com.soundcloud.java.collections.PropertySet;
import com.soundcloud.java.optional.Optional;

public abstract class SuggestionItem {

    enum Kind {
        SearchItem, TrackItem, UserItem, PlaylistItem, AutocompletionItem
    }

    abstract Kind kind();

    public abstract String userQuery();

    Urn getUrn() {
        return Urn.NOT_SET;
    }

    static SuggestionItem forSearch(String query) {
        return new AutoValue_SuggestionItem_AutocompletionItem(Kind.AutocompletionItem,
                                                               query,
                                                               query,
                                                               query,
                                                               Optional.<Urn>absent());
    }

    static SuggestionItem forLegacySearch(String query) {
        return new AutoValue_SuggestionItem_Default(Kind.SearchItem, query);
    }

    static SuggestionItem forUser(PropertySet source, String query) {
        return new AutoValue_SearchSuggestionItem(Kind.UserItem, query, source);
    }

    static SuggestionItem forTrack(PropertySet source, String query) {
        return new AutoValue_SearchSuggestionItem(Kind.TrackItem, query, source);
    }

    static SuggestionItem forPlaylist(PropertySet source, String query) {
        return new AutoValue_SearchSuggestionItem(Kind.PlaylistItem, query, source);
    }

    static SuggestionItem forAutocompletion(Autocompletion autocompletion, String userQuery, Optional<Urn> queryUrn) {
        return new AutoValue_SuggestionItem_AutocompletionItem(Kind.AutocompletionItem,
                                                               userQuery,
                                                               autocompletion.apiQuery(),
                                                               autocompletion.output(),
                                                               queryUrn);
    }

    @AutoValue
    abstract static class Default extends SuggestionItem {

    }

    @AutoValue
    abstract static class AutocompletionItem extends SuggestionItem {
        abstract String apiQuery();

        abstract String output();

        abstract Optional<Urn> queryUrn();
    }
}
