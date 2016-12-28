package com.soundcloud.android.search.suggestions;

import static com.soundcloud.android.search.suggestions.SuggestionItem.Kind;
import static com.soundcloud.android.search.suggestions.SuggestionItem.forLegacySearch;
import static org.assertj.core.api.Assertions.assertThat;

import com.soundcloud.android.model.Urn;
import com.soundcloud.java.optional.Optional;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class SuggestionItemTest {

    private static final String SEARCH_QUERY = "search_query";

    @Test
    public void shouldBuildCorrectSuggestionItemKind() {
        final SuggestionItem searchItem = forLegacySearch(SEARCH_QUERY);
        final SuggestionItem trackItem = SearchSuggestionItem.forTrack(Urn.forTrack(123L), Optional.absent(), SEARCH_QUERY, Optional.absent(), SEARCH_QUERY);
        final SuggestionItem userItem = SearchSuggestionItem.forUser(Urn.forUser(123L), Optional.absent(), SEARCH_QUERY, Optional.absent(), SEARCH_QUERY);

        assertThat(searchItem.kind()).isEqualTo(Kind.SearchItem);
        assertThat(trackItem.kind()).isEqualTo(Kind.TrackItem);
        assertThat(userItem.kind()).isEqualTo(Kind.UserItem);
    }

}
