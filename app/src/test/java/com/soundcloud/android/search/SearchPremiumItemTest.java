package com.soundcloud.android.search;

import static com.soundcloud.android.search.SearchPremiumItem.PREMIUM_URN;
import static org.assertj.core.api.Java6Assertions.assertThat;

import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playlists.PlaylistItem;
import com.soundcloud.android.presentation.ListItem;
import com.soundcloud.android.presentation.PlayableItem;
import com.soundcloud.android.testsupport.PlaylistFixtures;
import com.soundcloud.android.testsupport.TrackFixtures;
import com.soundcloud.android.testsupport.UserFixtures;
import com.soundcloud.android.tracks.TrackItem;
import com.soundcloud.android.users.UserItem;
import com.soundcloud.java.optional.Optional;
import org.junit.Before;
import org.junit.Test;

import java.util.Collections;

public class SearchPremiumItemTest {

    private static final Urn TRACK_URN = Urn.forTrack(1L);
    private static final Urn PLAYLIST_URN = Urn.forPlaylist(2L);
    private static final Urn USER_URN = Urn.forUser(3L);
    private static final int RESULTS_COUNT = 10;

    private SearchPremiumItem searchPremiumItem;

    @Before
    public void setUp() {
        final PlayableItem playableItem = TrackFixtures.trackItem(TRACK_URN);
        searchPremiumItem = buildWithTrackItem(playableItem);
    }

    @Test
    public void shouldHaveCorrectUrn() {
        assertThat(searchPremiumItem.getUrn()).isEqualTo(PREMIUM_URN);
    }

    @Test
    public void shouldBuildTrackAsFirstItem() {
        final PlayableItem playableItem = TrackFixtures.trackItem(TRACK_URN);
        searchPremiumItem = buildWithTrackItem(playableItem);

        assertThat(searchPremiumItem.getFirstItem()).isInstanceOf(TrackItem.class);
        assertThat(searchPremiumItem.getFirstItem().getUrn()).isEqualTo(TRACK_URN);
    }

    @Test
    public void shouldBuildPlaylistAsFirstItem() {
        final PlayableItem playableItem = PlaylistFixtures.playlistItem(PLAYLIST_URN);
        searchPremiumItem = buildWithTrackItem(playableItem);

        assertThat(searchPremiumItem.getFirstItem()).isInstanceOf(PlaylistItem.class);
        assertThat(searchPremiumItem.getFirstItem().getUrn()).isEqualTo(PLAYLIST_URN);
    }

    @Test
    public void shouldBuildUserAsFirstItem() {
        final UserItem userItem = UserFixtures.userItem(USER_URN);
        searchPremiumItem = buildWithTrackItem(userItem);

        assertThat(searchPremiumItem.getFirstItem()).isInstanceOf(UserItem.class);
        assertThat(searchPremiumItem.getFirstItem().getUrn()).isEqualTo(USER_URN);
    }

    @Test
    public void shouldSetTrackPlayingIfFirstItemIsTrack() {
        final PlayableItem playableItem = TrackFixtures.trackItem(TRACK_URN);
        searchPremiumItem = buildWithTrackItem(playableItem);

        assertThat(firstItem().isPlaying()).isFalse();

        searchPremiumItem.setTrackIsPlaying(TRACK_URN);
        assertThat(firstItem().isPlaying()).isTrue();
    }

    private TrackItem firstItem() {
        return (TrackItem) searchPremiumItem.getFirstItem();
    }

    @Test
    public void shouldNotSetTrackPlayingWithIncorrectUrn() {
        final PlayableItem playableItem = TrackFixtures.trackItem(TRACK_URN);
        searchPremiumItem = buildWithTrackItem(playableItem);

        final TrackItem trackItem = firstItem();
        assertThat(trackItem.isPlaying()).isFalse();

        searchPremiumItem.setTrackIsPlaying(USER_URN);
        assertThat(trackItem.isPlaying()).isFalse();
    }

    private SearchPremiumItem buildWithTrackItem(ListItem listItem) {
        return new SearchPremiumItem(Collections.singletonList(listItem), Optional.absent(), RESULTS_COUNT);
    }
}
