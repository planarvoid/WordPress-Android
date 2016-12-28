package com.soundcloud.android.search.suggestions;

import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.google.common.collect.Lists;
import com.soundcloud.android.configuration.experiments.AutocompleteConfig;
import com.soundcloud.android.model.Urn;
import com.soundcloud.java.optional.Optional;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.List;

@RunWith(MockitoJUnitRunner.class)
public class SearchSuggestionFilteringTest {
    private static final String QUERY = "query";
    private static final SuggestionItem P = SearchSuggestionItem.forPlaylist(Urn.forPlaylist(123L), Optional.absent(), QUERY, Optional.absent(), QUERY);
    private static final SuggestionItem U = SearchSuggestionItem.forUser(Urn.forUser(123L), Optional.absent(), QUERY, Optional.absent(), QUERY);
    private static final SuggestionItem T = SearchSuggestionItem.forTrack(Urn.forTrack(123L), Optional.absent(), QUERY, Optional.absent(), QUERY);

    @Mock private AutocompleteConfig autocompleteConfig;
    private SearchSuggestionFiltering searchSuggestionFiltering;

    @Before
    public void setUp() {
        this.searchSuggestionFiltering = new SearchSuggestionFiltering(autocompleteConfig);
    }

    @Test
    public void returnsTopFiveItemsWhenFeatureDisabled() {
        when(autocompleteConfig.isQueriesOnlyVariant()).thenReturn(false);
        when(autocompleteConfig.isShortcutsAndQueriesVariant()).thenReturn(false);
        when(autocompleteConfig.isFeatureFlagEnabled()).thenReturn(false);
        assertThat(searchSuggestionFiltering.filtered(list(P, P, P, U, U, U, T, T, T))).isEqualTo(list(P, P, P, U, U));
    }

    @Test
    public void returnsAllItemsWhenFeatureDisabledAndLessThanFiveItems() {
        when(autocompleteConfig.isQueriesOnlyVariant()).thenReturn(false);
        when(autocompleteConfig.isShortcutsAndQueriesVariant()).thenReturn(false);
        when(autocompleteConfig.isFeatureFlagEnabled()).thenReturn(false);
        assertThat(searchSuggestionFiltering.filtered(list(P, U, T))).isEqualTo(list(P, U, T));
    }

    @Test
    public void filtersAllItemsWhenVariantIsQueriesOnly() {
        when(autocompleteConfig.isQueriesOnlyVariant()).thenReturn(true);
        assertThat(searchSuggestionFiltering.filtered(list(P, P, P, U, U, U, T, T, T))).isEqualTo(emptyList());
    }

    @Test
    public void filtersToThreeItemsPreferingUserOverTrackOverPlaylistWhenVariantIsShortcutsAndQueries() {
        when(autocompleteConfig.isQueriesOnlyVariant()).thenReturn(false);
        when(autocompleteConfig.isShortcutsAndQueriesVariant()).thenReturn(true);
        when(autocompleteConfig.isFeatureFlagEnabled()).thenReturn(false);

        assertFiltersCorrectly();
    }

    @Test
    public void filtersToThreeItemsPreferingUserOverTrackOverPlaylistWhenFeatureFlagIsEnabled() {
        when(autocompleteConfig.isQueriesOnlyVariant()).thenReturn(false);
        when(autocompleteConfig.isFeatureFlagEnabled()).thenReturn(true);

        assertFiltersCorrectly();
    }


    private void assertFiltersCorrectly() {
        assertThat(searchSuggestionFiltering.filtered(list(P, P, P, U, U, U, T, T, T))).isEqualTo(list(U, U, U));
        assertThat(searchSuggestionFiltering.filtered(list(P, P, P, U, U, T, T, T))).isEqualTo(list(U, U, T));
        assertThat(searchSuggestionFiltering.filtered(list(P, P, P, U, T))).isEqualTo(list(U, T, P));
        assertThat(searchSuggestionFiltering.filtered(list(P, P, P, U, U))).isEqualTo(list(U, U, P));
        assertThat(searchSuggestionFiltering.filtered(list(P, P, P, U))).isEqualTo(list(U, P, P));
        assertThat(searchSuggestionFiltering.filtered(list(T, U))).isEqualTo(list(U, T));
    }

    @SafeVarargs
    private static <T> List<T> list(T... items) {
        return Lists.newArrayList(items);
    }
}
