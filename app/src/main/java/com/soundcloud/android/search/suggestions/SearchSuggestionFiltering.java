package com.soundcloud.android.search.suggestions;

import static com.soundcloud.android.search.suggestions.SuggestionItem.Kind.PlaylistItem;
import static com.soundcloud.android.search.suggestions.SuggestionItem.Kind.TrackItem;
import static com.soundcloud.android.search.suggestions.SuggestionItem.Kind.UserItem;
import static com.soundcloud.java.collections.Iterables.concat;
import static com.soundcloud.java.collections.Iterables.filter;
import static com.soundcloud.java.collections.Lists.newArrayList;
import static java.util.Collections.emptyList;

import com.soundcloud.android.configuration.experiments.AutocompleteConfig;

import javax.inject.Inject;
import java.util.List;

public class SearchSuggestionFiltering {
    private static final int NUM_SUGGESTIONS_WITH_AUTOCOMPLETE = 3;
    private static final int NUM_SUGGESTIONS_WITHOUT_AUTOCOMPLETE = 5;
    private final AutocompleteConfig autocompleteConfig;

    @Inject
    SearchSuggestionFiltering(AutocompleteConfig autocompleteConfig) {
        this.autocompleteConfig = autocompleteConfig;
    }

    List<SuggestionItem> filtered(List<SuggestionItem> suggestionItems) {
        if (autocompleteConfig.isQueriesOnlyVariant()) {
            return emptyList();
        } else if (autocompleteConfig.isFeatureFlagEnabled() || autocompleteConfig.isShortcutsAndQueriesVariant()) {
            return prioritizedFilterToThreeItems(suggestionItems);
        } else {
            return topFive(suggestionItems);
        }
    }

    @SuppressWarnings("unchecked")
    private List<SuggestionItem> prioritizedFilterToThreeItems(List<SuggestionItem> suggestionItems) {
        final Iterable<SuggestionItem> users = filter(suggestionItems, item -> item.kind() == UserItem);
        final Iterable<SuggestionItem> tracks = filter(suggestionItems, item -> item.kind() == TrackItem);
        final Iterable<SuggestionItem> playlists = filter(suggestionItems, item -> item.kind() == PlaylistItem);
        final Iterable<SuggestionItem> priorityOrdered = concat(users, tracks, playlists);
        return truncate(newArrayList(priorityOrdered), NUM_SUGGESTIONS_WITH_AUTOCOMPLETE);
    }

    private List<SuggestionItem> topFive(List<SuggestionItem> suggestionItems) {
        return truncate(suggestionItems, NUM_SUGGESTIONS_WITHOUT_AUTOCOMPLETE);
    }

    private List<SuggestionItem> truncate(List<SuggestionItem> suggestionItems, int size) {
        return suggestionItems.subList(0, Math.min(size, suggestionItems.size()));
    }

}
