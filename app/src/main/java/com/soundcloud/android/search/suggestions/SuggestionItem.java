package com.soundcloud.android.search.suggestions;

import com.google.auto.value.AutoValue;
import com.soundcloud.android.model.Urn;
import com.soundcloud.java.optional.Optional;

public abstract class SuggestionItem {

    public enum Kind {
        SearchItem, TrackItem, UserItem, PlaylistItem, AutocompletionItem
    }

    public abstract Kind kind();

    public abstract String userQuery();

    static SuggestionItem forLegacySearch(String query) {
        return new AutoValue_SuggestionItem_Default(Kind.SearchItem, query);
    }

    static SuggestionItem fromSearchSuggestion(SearchSuggestion searchSuggestion, String query) {
        final Optional<String> imageUrlTemplate = searchSuggestion.getImageUrlTemplate();
        final String displayText = searchSuggestion.getQuery();
        final Optional<SuggestionHighlight> suggestionHighlightOptional = searchSuggestion.getHighlights();
        final Urn urn = searchSuggestion.getUrn();
        if (urn.isTrack()) {
            return SearchSuggestionItem.forTrack(urn, imageUrlTemplate, query, suggestionHighlightOptional, displayText);
        } else if (urn.isUser()) {
            return SearchSuggestionItem.forUser(urn, imageUrlTemplate, query, suggestionHighlightOptional, displayText);
        } else if (urn.isPlaylist()) {
            return SearchSuggestionItem.forPlaylist(urn, imageUrlTemplate, query, suggestionHighlightOptional, displayText);
        } else {
            throw new IllegalStateException("Unexpected suggestion item type.");
        }
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
