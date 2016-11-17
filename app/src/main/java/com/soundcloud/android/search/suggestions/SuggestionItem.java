package com.soundcloud.android.search.suggestions;

import com.google.auto.value.AutoValue;
import com.soundcloud.android.model.Urn;
import com.soundcloud.java.collections.PropertySet;

abstract class SuggestionItem {

    enum Kind {
        SearchItem, TrackItem, UserItem, AutocompletionItem
    }

    abstract Kind kind();
    abstract String query();

    Urn getUrn() {
        return Urn.NOT_SET;
    }

    static SuggestionItem forSearch(String query) {
        return new AutoValue_SuggestionItem_Default(Kind.SearchItem, query);
    }

    static SuggestionItem forUser(PropertySet source, String query) {
        return new AutoValue_SearchSuggestionItem(Kind.UserItem, query, source);
    }

    static SuggestionItem forTrack(PropertySet source, String query) {
        return new AutoValue_SearchSuggestionItem(Kind.TrackItem, query, source);
    }

    static SuggestionItem forAutocompletion(Autocompletion autocompletion, String queryUrn) {
        return new AutoValue_SuggestionItem_AutocompletionItem(Kind.AutocompletionItem,
                                                               autocompletion.query(),
                                                               autocompletion.output(),
                                                               queryUrn);
    }

    @AutoValue
    abstract static class Default extends SuggestionItem {

    }

    @AutoValue
    abstract static class AutocompletionItem extends SuggestionItem {
        abstract String output();
        abstract String queryUrn();
    }
}
