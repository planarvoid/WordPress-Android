package com.soundcloud.android.playback.playqueue;

import static com.soundcloud.java.collections.Lists.newArrayList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.analytics.ScreenProvider;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.PlayerUICommand;
import com.soundcloud.android.main.Screen;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playback.PlayQueueManager;
import com.soundcloud.android.playback.PlaySessionSource;
import com.soundcloud.android.playback.PlaybackInitiator;
import com.soundcloud.android.playback.PlaybackResult;
import com.soundcloud.android.playback.ui.view.PlaybackToastHelper;
import com.soundcloud.android.playlists.PlaylistOperations;
import com.soundcloud.android.playlists.PlaylistWithTracks;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.testsupport.fixtures.ModelFixtures;
import com.soundcloud.android.tracks.TrackItem;
import com.soundcloud.rx.eventbus.TestEventBus;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import rx.Observable;

import java.util.List;

public class PlayQueueHelperTest extends AndroidUnitTest {

    private TestEventBus eventBus = new TestEventBus();
    @Mock private PlayQueueManager playQueueManager;
    @Mock private PlaylistOperations playlistOperations;
    @Mock private PlaybackToastHelper playbackToastHelper;
    @Mock private PlaybackInitiator playbackInitiator;
    @Mock private ScreenProvider screenProvider;
    private PlayQueueHelper playQueueHelper;
    private List<Urn> trackList;
    private Urn playlistUrn = Urn.forPlaylist(12345L);
    private PlaySessionSource playSessionSource = PlaySessionSource.forPlayNext(Screen.STREAM.get());

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        Urn track1 = Urn.forTrack(1L);
        Urn track2 = Urn.forTrack(2L);
        trackList = newArrayList(track1, track2);
        when(screenProvider.getLastScreenTag()).thenReturn(Screen.STREAM.get());
        playQueueHelper = new PlayQueueHelper(playQueueManager, playlistOperations, playbackToastHelper, eventBus,
                                              playbackInitiator, screenProvider);
    }

    @Test
    public void shouldIntialisePlayQueue() {
        when(playQueueManager.isQueueEmpty()).thenReturn(true);
        when(playlistOperations.trackUrnsForPlayback(eq(playlistUrn)))
                .thenReturn(Observable.just(trackList));
        when(playbackInitiator.playTracks(trackList, 0, playSessionSource))
                .thenReturn(Observable.just(PlaybackResult.success()));

        playQueueHelper.playNext(playlistUrn);

        verify(playlistOperations, times(1)).trackUrnsForPlayback(playlistUrn);
        verify(playbackInitiator, times(1)).playTracks(trackList, 0, playSessionSource);
        PlayerUICommand playerUICommand = eventBus.lastEventOn(EventQueue.PLAYER_COMMAND);
        assertThat(playerUICommand).isEqualTo(PlayerUICommand.expandPlayer());
    }

    @Test
    public void shouldInsertIntoPlayQueue() {
        final List<TrackItem> trackItemsList = ModelFixtures.trackItems(2);
        final PlaylistWithTracks playlistWithTracks = new PlaylistWithTracks(ModelFixtures.playlist(), trackItemsList);
        when(playlistOperations.playlist(eq(playlistWithTracks.getUrn()))).thenReturn(Observable.just(playlistWithTracks));
        when(playQueueManager.isQueueEmpty()).thenReturn(false);

        playQueueHelper.playNext(playlistWithTracks.getUrn());

        verify(playQueueManager, times(1)).insertNext(playlistWithTracks.getTracksUrn());
    }

}
