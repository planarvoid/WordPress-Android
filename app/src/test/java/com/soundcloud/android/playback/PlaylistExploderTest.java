package com.soundcloud.android.playback;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.events.CurrentPlayQueueItemEvent;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.PlayQueueEvent;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playlists.PlaylistOperations;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.testsupport.fixtures.TestPlayQueueItem;
import com.soundcloud.rx.eventbus.TestEventBus;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import rx.Observable;
import rx.subjects.PublishSubject;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class PlaylistExploderTest extends AndroidUnitTest {

    @Mock private PlaylistOperations playlistOperations;
    @Mock private PlayQueueManager playQueueManager;

    private TestEventBus eventBus;
    private PlayQueueItem trackPlayQueueItem;

    private PlaylistExploder playlistExploder;

    @Before
    public void setUp() throws Exception {
        trackPlayQueueItem = TestPlayQueueItem.createTrack(Urn.forTrack(123));
        eventBus = new TestEventBus();
        playlistExploder = new PlaylistExploder(eventBus, playlistOperations, playQueueManager);
        playlistExploder.subscribe();
    }

    @Test
    public void insertsPlaylistTracksForSurroundingPlaylists() {
        final Urn playlistUrn1 = Urn.forPlaylist(123);
        final Urn playlistUrn2 = Urn.forPlaylist(456);
        final Urn playlistUrn3 = Urn.forPlaylist(789);
        final List<Urn> trackUrns1 = Collections.singletonList(Urn.forTrack(123));
        final List<Urn> trackUrns2 = Collections.singletonList(Urn.forTrack(456));
        final List<Urn> trackUrns3 = Collections.singletonList(Urn.forTrack(789));
        when(playQueueManager.getPlayQueueItems(0, PlaylistExploder.PLAYLIST_LOOKAHEAD_COUNT))
                .thenReturn(Arrays.asList(playlistUrn1, playlistUrn2));
        when(playQueueManager.getPreviousPlayQueueItems(PlaylistExploder.PLAYLIST_LOOKBEHIND_COUNT))
                .thenReturn(Arrays.asList(playlistUrn3));

        when(playlistOperations.trackUrnsForPlayback(playlistUrn1)).thenReturn(Observable.just(trackUrns1));
        when(playlistOperations.trackUrnsForPlayback(playlistUrn2)).thenReturn(Observable.just(trackUrns2));
        when(playlistOperations.trackUrnsForPlayback(playlistUrn3)).thenReturn(Observable.just(trackUrns3));

        eventBus.publish(EventQueue.CURRENT_PLAY_QUEUE_ITEM,
                         CurrentPlayQueueItemEvent.fromPositionChanged(trackPlayQueueItem, Urn.NOT_SET, 0));

        verify(playQueueManager).insertPlaylistTracks(playlistUrn1, trackUrns1);
        verify(playQueueManager).insertPlaylistTracks(playlistUrn2, trackUrns2);
        verify(playQueueManager).insertPlaylistTracks(playlistUrn3, trackUrns3);
    }

    @Test
    public void playlistLoadRetriesOnTrackChangeAfterError() {
        final Urn playlistUrn1 = Urn.forPlaylist(123);
        final List<Urn> trackUrns1 = Collections.singletonList(Urn.forTrack(123));
        when(playQueueManager.getPlayQueueItems(anyInt(), anyInt()))
                .thenReturn(Arrays.asList(playlistUrn1));
        when(playlistOperations.trackUrnsForPlayback(playlistUrn1)).thenReturn(
                Observable.error(new IOException()), Observable.just(trackUrns1));

        eventBus.publish(EventQueue.CURRENT_PLAY_QUEUE_ITEM,
                         CurrentPlayQueueItemEvent.fromPositionChanged(trackPlayQueueItem, Urn.NOT_SET, 0));
        verify(playQueueManager, never()).insertPlaylistTracks(any(Urn.class), anyList());

        eventBus.publish(EventQueue.CURRENT_PLAY_QUEUE_ITEM,
                         CurrentPlayQueueItemEvent.fromPositionChanged(trackPlayQueueItem, Urn.NOT_SET, 0));
        verify(playQueueManager).insertPlaylistTracks(playlistUrn1, trackUrns1);
    }

    @Test
    public void playlistLoadsAreUnsubscribedOnQueueChange() {
        final Urn playlistUrn1 = Urn.forPlaylist(123);
        final Urn playlistUrn2 = Urn.forPlaylist(456);
        final PublishSubject<List<Urn>> playlistLoad1 = PublishSubject.create();
        final PublishSubject<List<Urn>> playlistLoad2 = PublishSubject.create();
        final PublishSubject<List<Urn>> playlistLoad3 = PublishSubject.create();
        final PublishSubject<List<Urn>> playlistLoad4 = PublishSubject.create();

        when(playQueueManager.getPlayQueueItems(anyInt(), anyInt()))
                .thenReturn(Arrays.asList(playlistUrn1, playlistUrn2));
        when(playlistOperations.trackUrnsForPlayback(playlistUrn1)).thenReturn(playlistLoad1);
        when(playlistOperations.trackUrnsForPlayback(playlistUrn2)).thenReturn(playlistLoad2);

        eventBus.publish(EventQueue.CURRENT_PLAY_QUEUE_ITEM,
                         CurrentPlayQueueItemEvent.fromPositionChanged(trackPlayQueueItem, Urn.NOT_SET, 0));

        assertThat(playlistLoad1.hasObservers()).isTrue();
        assertThat(playlistLoad2.hasObservers()).isTrue();

        when(playlistOperations.trackUrnsForPlayback(playlistUrn1)).thenReturn(playlistLoad3);
        when(playlistOperations.trackUrnsForPlayback(playlistUrn2)).thenReturn(playlistLoad4);

        eventBus.publish(EventQueue.PLAY_QUEUE, PlayQueueEvent.fromNewQueue(Urn.NOT_SET));

        assertThat(playlistLoad1.hasObservers()).isFalse();
        assertThat(playlistLoad2.hasObservers()).isFalse();
        assertThat(playlistLoad3.hasObservers()).isTrue();
        assertThat(playlistLoad4.hasObservers()).isTrue();

    }

}
