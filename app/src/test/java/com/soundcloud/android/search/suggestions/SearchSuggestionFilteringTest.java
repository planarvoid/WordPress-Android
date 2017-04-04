package com.soundcloud.android.search.suggestions;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.common.collect.Lists;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.properties.FeatureFlags;
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

    @Mock private FeatureFlags featureFlags;
    private SearchSuggestionFiltering searchSuggestionFiltering;

    @Before
    public void setUp() {
        this.searchSuggestionFiltering = new SearchSuggestionFiltering();
    }
    @Test
    public void filtersToThreeItemsPreferingUserOverTrackOverPlaylistWhenFeatureFlagIsEnabled() {
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
