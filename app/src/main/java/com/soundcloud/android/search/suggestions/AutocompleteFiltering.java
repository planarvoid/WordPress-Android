package com.soundcloud.android.search.suggestions;

import static com.soundcloud.android.search.suggestions.SuggestionItem.Kind.PlaylistItem;
import static com.soundcloud.android.search.suggestions.SuggestionItem.Kind.TrackItem;
import static com.soundcloud.android.search.suggestions.SuggestionItem.Kind.UserItem;
import static java.util.Collections.emptyList;

import com.soundcloud.android.configuration.experiments.AutocompleteConfig;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;

public class AutocompleteFiltering {
    private final AutocompleteConfig autocompleteConfig;

    @Inject
    public AutocompleteFiltering(AutocompleteConfig autocompleteConfig) {
        this.autocompleteConfig = autocompleteConfig;
    }

    public List<SuggestionItem> filter(List<SuggestionItem> suggestionItems) {
        if (autocompleteConfig.isQueriesOnlyVariant()) {
            return emptyList();
        } else if (autocompleteConfig.isShortcutsAndQueriesVariant()) {
            return maxTwoFollowsAndTwoLikes(suggestionItems);
        }
        return suggestionItems;
    }

    private List<SuggestionItem> maxTwoFollowsAndTwoLikes(List<SuggestionItem> suggestionItems) {
        int followCount = 0;
        int likeCount = 0;
        final ArrayList<SuggestionItem> result = new ArrayList<>(4);

        for (int i = 0; i < suggestionItems.size() && ((followCount < 2) || (likeCount < 2)); i++) {
            final SuggestionItem suggestionItem = suggestionItems.get(i);
            if (suggestionItem.kind().equals(UserItem) && followCount < 2) {
                followCount++;
                result.add(suggestionItem);
            } else if ((suggestionItem.kind().equals(TrackItem) || suggestionItem.kind()
                                                                                 .equals(PlaylistItem)) && likeCount < 2) {
                likeCount++;
                result.add(suggestionItem);
            }
        }
        return result;
    }

}
