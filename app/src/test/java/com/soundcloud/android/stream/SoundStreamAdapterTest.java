package com.soundcloud.android.stream;

import static com.soundcloud.android.Expect.expect;
import static org.mockito.Mockito.verify;

import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.PlayQueueEvent;
import com.soundcloud.android.events.PlayableChangedEvent;
import com.soundcloud.android.model.PlayableProperty;
import com.soundcloud.android.model.Playlist;
import com.soundcloud.android.model.PropertySet;
import com.soundcloud.android.model.TrackUrn;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.rx.eventbus.TestEventBus;
import com.soundcloud.android.view.adapters.PlaylistItemPresenter;
import com.soundcloud.android.view.adapters.TrackItemPresenter;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;

@RunWith(SoundCloudTestRunner.class)
public class SoundStreamAdapterTest {

    private SoundStreamAdapter adapter;
    private TestEventBus eventBus = new TestEventBus();

    @Mock
    private TrackItemPresenter trackItemPresenter;
    @Mock
    private PlaylistItemPresenter playlistItemPresenter;

    @Before
    public void setup() {
        adapter = new SoundStreamAdapter(trackItemPresenter, playlistItemPresenter, eventBus);
    }

    @Test
    public void shouldReportTwoDifferentItemViewTypes() {
        expect(adapter.getViewTypeCount()).toEqual(2);
    }

    @Test
    public void shouldReportTrackTypeForTracks() {
        adapter.addItem(PropertySet.from(PlayableProperty.URN.bind(Urn.forTrack(123))));
        expect(adapter.getItemViewType(0)).toEqual(SoundStreamAdapter.TRACK_ITEM_TYPE);
    }

    @Test
    public void shouldReportPlaylistTypeForPlaylists() {
        adapter.addItem(PropertySet.from(PlayableProperty.URN.bind(Urn.forPlaylist(123))));
        expect(adapter.getItemViewType(0)).toEqual(SoundStreamAdapter.PLAYLIST_ITEM_TYPE);
    }

    @Test
    public void trackChangedEventShouldUpdateTrackPresenterWithCurrentlyPlayingTrack() {
        final TrackUrn playingTrack = Urn.forTrack(123L);
        adapter.onViewCreated();
        eventBus.publish(EventQueue.PLAY_QUEUE, PlayQueueEvent.fromTrackChange(playingTrack));
        verify(trackItemPresenter).setPlayingTrack(playingTrack);
    }

    @Test
    public void newQueueEventShouldUpdateTrackPresenterWithCurrentlyPlayingTrack() {
        final TrackUrn playingTrack = Urn.forTrack(123L);
        adapter.onViewCreated();
        eventBus.publish(EventQueue.PLAY_QUEUE, PlayQueueEvent.fromNewQueue(playingTrack));
        verify(trackItemPresenter).setPlayingTrack(playingTrack);
    }

    @Test
    public void playableChangedEventShouldUpdateAdapterToReflectTheLatestLikeStatus() {
        final PropertySet unlikedPlaylist = buildUnlikedPlaylist(123);
        final PropertySet likedPlaylist = buildLikedPlaylist(456);

        adapter.addItem(unlikedPlaylist);
        adapter.addItem(likedPlaylist);
        adapter.onViewCreated();

        publishPlaylistLikeEvent(123); // 123 is now liked, too

        expect(adapter.getItems()).toContainExactly(
                unlikedPlaylist.merge(
                        PropertySet.from(
                                PlayableProperty.IS_LIKED.bind(true),
                                PlayableProperty.LIKES_COUNT.bind(1)
                        )),
                likedPlaylist
        );
    }

    @Test
    public void shouldUnsubscribeFromEventBusInOnDestroyView() {
        adapter.onViewCreated();
        adapter.onDestroyView();
        eventBus.verifyUnsubscribed();
    }

    private void publishPlaylistLikeEvent(long id) {
        Playlist playlist = new Playlist(id);
        playlist.user_like = true;
        playlist.likes_count = 1;
        eventBus.publish(EventQueue.PLAYABLE_CHANGED, PlayableChangedEvent.forLike(playlist, true));
    }

    private PropertySet buildLikedPlaylist(long id) {
        return PropertySet.from(
                PlayableProperty.URN.bind(Urn.forPlaylist(id)),
                PlayableProperty.IS_LIKED.bind(true),
                PlayableProperty.LIKES_COUNT.bind(1)
        );
    }

    private PropertySet buildUnlikedPlaylist(long id) {
        return PropertySet.from(
                PlayableProperty.URN.bind(Urn.forPlaylist(id)),
                PlayableProperty.IS_LIKED.bind(false),
                PlayableProperty.LIKES_COUNT.bind(0)
        );
    }
}