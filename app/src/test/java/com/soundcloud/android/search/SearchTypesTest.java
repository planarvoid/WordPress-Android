package com.soundcloud.android.search;

import static com.soundcloud.android.search.SearchType.ALBUMS;
import static com.soundcloud.android.search.SearchType.ALL;
import static com.soundcloud.android.search.SearchType.PLAYLISTS;
import static com.soundcloud.android.search.SearchType.TRACKS;
import static com.soundcloud.android.search.SearchType.USERS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.soundcloud.android.properties.FeatureFlags;
import com.soundcloud.android.properties.Flag;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class SearchTypesTest {
    @Mock private FeatureFlags featureFlags;

    @Test
    public void albumsAvailableWhenFeatureFlagEnabled() {
        when(featureFlags.isEnabled(Flag.ALBUMS)).thenReturn(true);
        final SearchTypes searchTypes = new SearchTypes(featureFlags);
        assertThat(searchTypes.available()).containsExactly(ALL, TRACKS, USERS, ALBUMS, PLAYLISTS);
    }

    @Test
    public void albumsNotAvailableWhenFeatureFlagDisabled() {
        when(featureFlags.isEnabled(Flag.ALBUMS)).thenReturn(false);
        final SearchTypes searchTypes = new SearchTypes(featureFlags);
        assertThat(searchTypes.available()).containsExactly(ALL, TRACKS, USERS, PLAYLISTS);
    }

    @Test
    public void canGetSearchTypeByIndex() {
        when(featureFlags.isEnabled(Flag.ALBUMS)).thenReturn(false);
        final SearchTypes searchTypes = new SearchTypes(featureFlags);
        assertThat(searchTypes.get(3)).isEqualTo(PLAYLISTS);
    }
}
