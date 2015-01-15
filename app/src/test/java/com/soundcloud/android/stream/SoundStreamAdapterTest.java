package com.soundcloud.android.stream;

import static com.soundcloud.android.Expect.expect;
import static org.mockito.Mockito.verify;

import com.soundcloud.android.events.CurrentPlayQueueTrackEvent;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.PlayableUpdatedEvent;
import com.soundcloud.android.model.PlayableProperty;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.rx.eventbus.TestEventBus;
import com.soundcloud.android.view.adapters.PlaylistItemPresenter;
import com.soundcloud.android.tracks.TrackItemPresenter;
import com.soundcloud.propeller.PropertySet;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;

import android.support.v4.app.Fragment;
import android.view.View;

@RunWith(SoundCloudTestRunner.class)
public class SoundStreamAdapterTest {

    private SoundStreamAdapter adapter;
    private TestEventBus eventBus = new TestEventBus();

    @Mock private TrackItemPresenter trackItemPresenter;
    @Mock private PlaylistItemPresenter playlistItemPresenter;
    @Mock private View view;
    @Mock private Fragment fragment;

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
        final Urn playingTrack = Urn.forTrack(123L);
        adapter.onViewCreated(fragment, view, null);
        eventBus.publish(EventQueue.PLAY_QUEUE_TRACK, CurrentPlayQueueTrackEvent.fromPositionChanged(playingTrack));
        verify(trackItemPresenter).setPlayingTrack(playingTrack);
    }

    @Test
    public void newQueueEventShouldUpdateTrackPresenterWithCurrentlyPlayingTrack() {
        final Urn playingTrack = Urn.forTrack(123L);
        adapter.onViewCreated(fragment, view, null);
        eventBus.publish(EventQueue.PLAY_QUEUE_TRACK, CurrentPlayQueueTrackEvent.fromNewQueue(playingTrack));
        verify(trackItemPresenter).setPlayingTrack(playingTrack);
    }

    @Test
    public void playableChangedEventShouldUpdateAdapterToReflectTheLatestLikeStatus() {
        final PropertySet unlikedPlaylist = buildUnlikedPlaylist(123L);
        final PropertySet likedPlaylist = buildLikedPlaylist(456L);

        adapter.addItem(unlikedPlaylist);
        adapter.addItem(likedPlaylist);
        adapter.onViewCreated(fragment, view, null);

        eventBus.publish(EventQueue.PLAYABLE_CHANGED, PlayableUpdatedEvent.forLike(Urn.forPlaylist(123L), true, 1));

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
        adapter.onViewCreated(fragment, view, null);
        adapter.onDestroyView(fragment);
        eventBus.verifyUnsubscribed();
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