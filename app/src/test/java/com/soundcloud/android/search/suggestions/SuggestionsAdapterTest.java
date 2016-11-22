package com.soundcloud.android.search.suggestions;

import static org.assertj.core.api.Assertions.assertThat;

import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.java.collections.PropertySet;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import java.util.Arrays;

public class SuggestionsAdapterTest extends AndroidUnitTest {

    private static final String SEARCH_QUERY = "query";

    @Mock private SearchSuggestionItemRenderer searchSuggestionItemRenderer;
    @Mock private TrackSuggestionItemRenderer trackSuggestionItemRenderer;
    @Mock private UserSuggestionItemRenderer userSuggestionItemRenderer;
    @Mock private PlaylistSuggestionItemRenderer playlistSuggestionItemRenderer;

    private SuggestionsAdapter adapter;

    @Before
    public void setUp() throws Exception {
        adapter = new SuggestionsAdapter(searchSuggestionItemRenderer,
                                         trackSuggestionItemRenderer,
                                         userSuggestionItemRenderer,
                                         playlistSuggestionItemRenderer);
    }

    @Test
    public void rendersCorrectViewTypes() {
        SuggestionItem searchItem = SuggestionItem.forSearch(SEARCH_QUERY);
        SuggestionItem trackItem = SuggestionItem.forTrack(PropertySet.create(), SEARCH_QUERY);
        SuggestionItem userItem = SuggestionItem.forUser(PropertySet.create(), SEARCH_QUERY);
        SuggestionItem playlistItem = SuggestionItem.forPlaylist(PropertySet.create(), SEARCH_QUERY);

        adapter.onNext(Arrays.asList(searchItem, trackItem, userItem, playlistItem));

        assertThat(adapter.getBasicItemViewType(0)).isEqualTo(SuggestionsAdapter.TYPE_SEARCH);
        assertThat(adapter.getBasicItemViewType(1)).isEqualTo(SuggestionsAdapter.TYPE_TRACK);
        assertThat(adapter.getBasicItemViewType(2)).isEqualTo(SuggestionsAdapter.TYPE_USER);
        assertThat(adapter.getBasicItemViewType(3)).isEqualTo(SuggestionsAdapter.TYPE_PLAYLIST);
    }

}
