package com.soundcloud.android.search.suggestions;

import com.google.auto.value.AutoValue;
import com.soundcloud.android.model.Urn;
import com.soundcloud.java.collections.PropertySet;
import com.soundcloud.java.optional.Optional;

public abstract class SuggestionItem {

    public enum Kind {
        SearchItem, TrackItem, UserItem, PlaylistItem, AutocompletionItem
    }

    public abstract Kind kind();

    public abstract String userQuery();

    Urn getUrn() {
        return Urn.NOT_SET;
    }

    static SuggestionItem forLegacySearch(String query) {
        return new AutoValue_SuggestionItem_Default(Kind.SearchItem, query);
    }

    static SuggestionItem fromPropertySet(PropertySet source, String query) {
        final Urn urn = source.get(SearchSuggestionProperty.URN);
        if (urn.isTrack()) {
            return SuggestionItem.forTrack(source, query);
        } else if (urn.isUser()) {
            return SuggestionItem.forUser(source, query);
        } else if (urn.isPlaylist()) {
            return SuggestionItem.forPlaylist(source, query);
        } else {
            throw new IllegalStateException("Unexpected suggestion item type.");
        }
    }

    public static SuggestionItem forUser(PropertySet source, String query) {
        return new AutoValue_SearchSuggestionItem(Kind.UserItem, query, source);
    }

    public static SuggestionItem forTrack(PropertySet source, String query) {
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
