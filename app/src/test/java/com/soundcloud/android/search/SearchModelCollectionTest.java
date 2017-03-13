package com.soundcloud.android.search;

import static com.soundcloud.java.optional.Optional.absent;
import static org.assertj.core.api.Assertions.assertThat;

import com.soundcloud.android.api.model.ApiPlaylist;
import com.soundcloud.android.api.model.ApiTrack;
import com.soundcloud.android.api.model.ApiUser;
import com.soundcloud.android.testsupport.fixtures.ModelFixtures;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.Collections;

@RunWith(MockitoJUnitRunner.class)
public class SearchModelCollectionTest {

    private static final int TRACK_RESULTS_COUNT = 10;
    private static final int PLAYLIST_RESULTS_COUNT = 10;
    private static final int USER_RESULTS_COUNT = 10;

    @Test
    public void premiumContentShouldNotBePresent() {
        final SearchModelCollection<ApiTrack> searchResultTracks =
                new SearchModelCollection<>(ModelFixtures.create(ApiTrack.class, 2),
                                            Collections.emptyMap());
        final SearchModelCollection<ApiPlaylist> searchResultPlaylists =
                new SearchModelCollection<>(ModelFixtures.create(ApiPlaylist.class, 2),
                                            Collections.emptyMap(), "queryUrn", null,
                                            TRACK_RESULTS_COUNT, PLAYLIST_RESULTS_COUNT, USER_RESULTS_COUNT);
        final SearchModelCollection<ApiUser> searchResultUsers =
                new SearchModelCollection<>(ModelFixtures.create(ApiUser.class, 1));

        assertThat(searchResultTracks.premiumContent()).isEqualTo(absent());
        assertThat(searchResultPlaylists.premiumContent()).isEqualTo(absent());
        assertThat(searchResultUsers.premiumContent()).isEqualTo(absent());
    }

    @Test
    public void premiumContentShouldBePresent() {
        final SearchModelCollection<ApiTrack> searchResultPremiumTracks =
                new SearchModelCollection<>(ModelFixtures.create(ApiTrack.class, 2),
                                            Collections.emptyMap());
        final SearchModelCollection<ApiTrack> searchResultTracks =
                new SearchModelCollection<>(ModelFixtures.create(ApiTrack.class, 2),
                                            Collections.emptyMap(), "queryUrn", searchResultPremiumTracks,
                                            TRACK_RESULTS_COUNT, PLAYLIST_RESULTS_COUNT, USER_RESULTS_COUNT);

        assertThat(searchResultTracks.premiumContent().get()).isEqualTo(searchResultPremiumTracks);
    }

    @Test
    public void shouldCalculateResultsCountFromTracksPlaylistsAndUsers() {
        final SearchModelCollection<ApiPlaylist> searchResultPlaylists =
                new SearchModelCollection<>(ModelFixtures.create(ApiPlaylist.class, 2),
                                            Collections.emptyMap(), "queryUrn", null,
                                            TRACK_RESULTS_COUNT, PLAYLIST_RESULTS_COUNT, USER_RESULTS_COUNT);

        assertThat(searchResultPlaylists.resultsCount()).isEqualTo(30);
    }
}
