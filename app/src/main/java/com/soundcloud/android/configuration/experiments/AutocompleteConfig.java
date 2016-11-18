package com.soundcloud.android.configuration.experiments;

import static com.soundcloud.android.configuration.experiments.ActiveExperiments.LISTENING_LAYER;
import static com.soundcloud.android.search.suggestions.SuggestionItem.Kind.PlaylistItem;
import static com.soundcloud.android.search.suggestions.SuggestionItem.Kind.TrackItem;
import static com.soundcloud.android.search.suggestions.SuggestionItem.Kind.UserItem;
import static java.util.Collections.emptyList;

import com.soundcloud.android.properties.FeatureFlags;
import com.soundcloud.android.properties.Flag;
import com.soundcloud.android.search.suggestions.SuggestionItem;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class AutocompleteConfig {
    private static final String NAME = "search_query_autocomplete_android";
    static final String VARIANT_CONTROL_A = "control_a";
    static final String VARIANT_CONTROL_B = "control_b";
    static final String VARIANT_SHORTCUTS_AND_QUERIES = "shortcuts_and_queries";
    static final String VARIANT_QUERIES_ONLY = "queries_only";

    static final ExperimentConfiguration CONFIGURATION = ExperimentConfiguration
            .fromName(LISTENING_LAYER,
                      NAME,
                      Arrays.asList(VARIANT_CONTROL_A,
                                    VARIANT_CONTROL_B,
                                    VARIANT_QUERIES_ONLY,
                                    VARIANT_SHORTCUTS_AND_QUERIES));

    private final ExperimentOperations experimentOperations;
    private final FeatureFlags featureFlags;

    @Inject
    AutocompleteConfig(ExperimentOperations experimentOperations, FeatureFlags featureFlags) {
        this.experimentOperations = experimentOperations;
        this.featureFlags = featureFlags;
    }

    public boolean isEnabled() {
        return isFeatureFlagEnabled() || isShortcutsAndQueriesVariant() || isQueriesOnlyVariant();
    }

    public List<SuggestionItem> filter(List<SuggestionItem> suggestionItems) {
        if (isQueriesOnlyVariant()) {
            return emptyList();
        } else if (isShortcutsAndQueriesVariant()) {
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

    private boolean isShortcutsAndQueriesVariant() {
        return VARIANT_SHORTCUTS_AND_QUERIES.equals(getVariant());
    }

    private boolean isQueriesOnlyVariant() {
        return VARIANT_QUERIES_ONLY.equals(getVariant());
    }

    private boolean isFeatureFlagEnabled() {
        return featureFlags.isEnabled(Flag.AUTOCOMPLETE);
    }

    private String getVariant() {
        return experimentOperations.getExperimentVariant(CONFIGURATION);
    }
}
