package com.soundcloud.android.search.suggestions;

import static com.soundcloud.android.search.suggestions.SuggestionItem.*;
import static org.assertj.core.api.Assertions.assertThat;

import com.soundcloud.java.collections.PropertySet;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class SuggestionItemTest {

    private static final String SEARCH_QUERY = "search_query";

    @Test
    public void shouldBuildCorrectSuggestionItemKind() {
        final SuggestionItem searchItem = forSearch(SEARCH_QUERY);
        final SuggestionItem trackItem = forTrack(PropertySet.create(), SEARCH_QUERY);
        final SuggestionItem userItem = forUser(PropertySet.create(), SEARCH_QUERY);

        assertThat(searchItem.getKind()).isEqualTo(Kind.SearchItem);
        assertThat(trackItem.getKind()).isEqualTo(Kind.TrackItem);
        assertThat(userItem.getKind()).isEqualTo(Kind.UserItem);
    }
}