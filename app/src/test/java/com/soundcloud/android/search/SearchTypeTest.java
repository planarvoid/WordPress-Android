package com.soundcloud.android.search;

import static com.soundcloud.android.search.SearchType.ALBUMS;
import static com.soundcloud.android.search.SearchType.ALL;
import static com.soundcloud.android.search.SearchType.PLAYLISTS;
import static com.soundcloud.android.search.SearchType.TRACKS;
import static com.soundcloud.android.search.SearchType.USERS;
import static org.assertj.core.api.Assertions.assertThat;

import com.soundcloud.android.properties.FeatureFlags;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class SearchTypeTest {
    @Mock private FeatureFlags featureFlags;

    @Test
    public void listsAllAvailableSearchTypes() {
        assertThat(SearchType.asList()).containsExactly(ALL, TRACKS, USERS, ALBUMS, PLAYLISTS);
    }

    @Test
    public void canGetSearchTypeByIndex() {
        assertThat(SearchType.get(3)).isEqualTo(ALBUMS);
    }
}
