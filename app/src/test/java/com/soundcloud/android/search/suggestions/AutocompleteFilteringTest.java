package com.soundcloud.android.search.suggestions;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.soundcloud.android.configuration.experiments.AutocompleteConfig;
import com.soundcloud.java.collections.PropertySet;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@RunWith(MockitoJUnitRunner.class)
public class AutocompleteFilteringTest {
    private static final String QUERY = "query";

    @Mock private AutocompleteConfig autocompleteConfig;
    private AutocompleteFiltering autocompleteFiltering;
    private List<SuggestionItem> suggestedItems;

    @Before
    public void setUp() {
        suggestedItems = new ArrayList<>(5);
        suggestedItems.add(SuggestionItem.forUser(PropertySet.create(), QUERY));
        suggestedItems.add(SuggestionItem.forTrack(PropertySet.create(), QUERY));
        suggestedItems.add(SuggestionItem.forUser(PropertySet.create(), QUERY));
        suggestedItems.add(SuggestionItem.forUser(PropertySet.create(), QUERY));
        suggestedItems.add(SuggestionItem.forTrack(PropertySet.create(), QUERY));

        this.autocompleteFiltering = new AutocompleteFiltering(autocompleteConfig);
    }

    @Test
    public void filtersNoItemWhenNoVariantSet() {
        when(autocompleteConfig.isQueriesOnlyVariant()).thenReturn(false);
        when(autocompleteConfig.isShortcutsAndQueriesVariant()).thenReturn(false);
        assertThat(autocompleteFiltering.filter(suggestedItems)).isEqualTo(suggestedItems);
    }

    @Test
    public void filtersAllItemsWhenVariantIsQueriesOnly() {
        when(autocompleteConfig.isQueriesOnlyVariant()).thenReturn(true);
        when(autocompleteConfig.isShortcutsAndQueriesVariant()).thenReturn(false);
        assertThat(autocompleteFiltering.filter(suggestedItems)).isEqualTo(Collections.emptyList());
    }

    @Test
    public void filtersToTwoUsersTwoTracksWhenVariantIsShortcutsAndQueries() {
        when(autocompleteConfig.isQueriesOnlyVariant()).thenReturn(false);
        when(autocompleteConfig.isShortcutsAndQueriesVariant()).thenReturn(true);
        ArrayList<SuggestionItem> expecteditems = new ArrayList<>(4);

        expecteditems.add(SuggestionItem.forUser(PropertySet.create(), QUERY));
        expecteditems.add(SuggestionItem.forTrack(PropertySet.create(), QUERY));
        expecteditems.add(SuggestionItem.forUser(PropertySet.create(), QUERY));
        expecteditems.add(SuggestionItem.forTrack(PropertySet.create(), QUERY));
        assertThat(autocompleteFiltering.filter(suggestedItems)).isEqualTo(expecteditems);
    }
}
