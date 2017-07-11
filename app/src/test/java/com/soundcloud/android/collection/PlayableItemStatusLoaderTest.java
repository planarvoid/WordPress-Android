package com.soundcloud.android.collection;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyIterable;
import static org.mockito.Mockito.when;

import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playlists.PlaylistItem;
import com.soundcloud.android.presentation.PlayableItem;
import com.soundcloud.android.testsupport.fixtures.ModelFixtures;
import com.soundcloud.android.tracks.TrackItem;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.HashMap;
import java.util.Map;

@RunWith(MockitoJUnitRunner.class)
public class PlayableItemStatusLoaderTest {

    @Mock private LoadPlaylistLikedStatuses loadPlaylistLikedStatuses;
    @Mock private LoadPlaylistRepostStatuses loadPlaylistRepostStatuses;
    @Mock private LoadTrackLikedStatuses loadTrackLikedStatuses;
    @Mock private LoadTrackRepostStatuses loadTrackRepostStatuses;

    private PlayableItemStatusLoader subject;
    private Map<Urn, Boolean> statusMap;
    private PlaylistItem playlistItem;
    private TrackItem trackItem;

    @Before
    public void setUp() throws Exception {
        subject = new PlayableItemStatusLoader(loadPlaylistLikedStatuses, loadPlaylistRepostStatuses,
                                               loadTrackLikedStatuses, loadTrackRepostStatuses);

        playlistItem = ModelFixtures.playlistItem();
        trackItem = ModelFixtures.trackItem();
        statusMap = new HashMap<>();
    }

    @Test
    public void shouldUpdatePlaylistLikedStatusToTrue() throws Exception {
        statusMap.put(playlistItem.getUrn(), true);
        when(loadPlaylistLikedStatuses.call(anyIterable())).thenReturn(statusMap);

        final Iterable<PlayableItem> playableItems = subject.call(singletonList(playlistItem));

        final PlayableItem updatedPlaylistItem = playableItems.iterator().next();
        assertThat(updatedPlaylistItem.getUrn()).isEqualTo(playlistItem.getUrn());
        assertThat(updatedPlaylistItem.isUserLike()).isTrue();
    }

    @Test
    public void shouldUpdatePlaylistLikedStatusToFalse() throws Exception {
        statusMap.put(playlistItem.getUrn(), false);
        when(loadPlaylistLikedStatuses.call(anyIterable())).thenReturn(statusMap);

        subject.call(singletonList(playlistItem));

        assertThat(playlistItem.isUserLike()).isFalse();
    }

    @Test
    public void shouldUpdatePlaylistRepostStatusToTrue() throws Exception {
        statusMap.put(playlistItem.getUrn(), true);
        when(loadPlaylistRepostStatuses.call(anyIterable())).thenReturn(statusMap);

        final Iterable<PlayableItem> playableItems = subject.call(singletonList(playlistItem));

        final PlayableItem updatedPlaylistItem = playableItems.iterator().next();
        assertThat(updatedPlaylistItem.getUrn()).isEqualTo(playlistItem.getUrn());
        assertThat(updatedPlaylistItem.isUserRepost()).isTrue();
    }

    @Test
    public void shouldUpdatePlaylistRepostStatusToFalse() throws Exception {
        statusMap.put(playlistItem.getUrn(), false);
        when(loadPlaylistRepostStatuses.call(anyIterable())).thenReturn(statusMap);

        subject.call(singletonList(playlistItem));

        assertThat(playlistItem.isUserRepost()).isFalse();
    }

    @Test
    public void shouldUpdateTrackLikedStatusToTrue() throws Exception {
        statusMap.put(trackItem.getUrn(), true);
        when(loadTrackLikedStatuses.call(anyIterable())).thenReturn(statusMap);

        final Iterable<PlayableItem> playableItems = subject.call(singletonList(trackItem));

        final PlayableItem next = playableItems.iterator().next();
        assertThat(next.getUrn()).isEqualTo(trackItem.getUrn());
        assertThat(next.isUserLike()).isTrue();
    }

    @Test
    public void shouldUpdateTrackLikedStatusToFalse() throws Exception {
        statusMap.put(trackItem.getUrn(), false);
        when(loadTrackLikedStatuses.call(anyIterable())).thenReturn(statusMap);

        subject.call(singletonList(trackItem));

        assertThat(trackItem.isUserLike()).isFalse();
    }

    @Test
    public void shouldUpdateTrackRepostStatusToTrue() throws Exception {
        statusMap.put(trackItem.getUrn(), true);
        when(loadTrackRepostStatuses.call(anyIterable())).thenReturn(statusMap);

        final Iterable<PlayableItem> playableItems = subject.call(singletonList(trackItem));

        final PlayableItem next = playableItems.iterator().next();
        assertThat(next.getUrn()).isEqualTo(trackItem.getUrn());
        assertThat(next.isUserRepost()).isTrue();
    }

    @Test
    public void shouldUpdateTrackRepostStatusToFalse() throws Exception {
        statusMap.put(trackItem.getUrn(), false);
        when(loadTrackRepostStatuses.call(anyIterable())).thenReturn(statusMap);

        subject.call(singletonList(trackItem));

        assertThat(trackItem.isUserRepost()).isFalse();
    }
}
