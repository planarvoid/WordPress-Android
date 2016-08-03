package com.soundcloud.android.playback.playqueue;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyList;
import static org.mockito.Matchers.anyListOf;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.analytics.ScreenProvider;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.PlayerUICommand;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playback.PlayQueueManager;
import com.soundcloud.android.playback.PlaySessionSource;
import com.soundcloud.android.playback.PlaybackInitiator;
import com.soundcloud.android.playback.PlaybackResult;
import com.soundcloud.android.playback.ui.view.PlaybackToastHelper;
import com.soundcloud.android.playlists.PlaylistOperations;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.java.collections.Lists;
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
    private Urn playlistUrn =Urn.forPlaylist(12345l);

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        Urn track1 = Urn.forTrack(1l);
        Urn track2 = Urn.forTrack(2l);
        trackList = Lists.newArrayList(track1, track2);
        playQueueHelper = new PlayQueueHelper(playQueueManager, playlistOperations, playbackToastHelper, eventBus,
                                              playbackInitiator, screenProvider);
    }

    @Test
    public void shouldIntialisePlayQueue() {
        when(playQueueManager.isQueueEmpty()).thenReturn(true);
        when(playlistOperations.trackUrnsForPlayback(eq(playlistUrn)))
                .thenReturn(Observable.just(trackList));
        when(playbackInitiator.playTracks(anyList(), eq(0), any(PlaySessionSource.class)))
                .thenReturn(Observable.just(PlaybackResult.success()));

        playQueueHelper.playNext(playlistUrn);

        verify(playlistOperations, times(1)).trackUrnsForPlayback(playlistUrn);
        verify(playbackInitiator, times(1)).playTracks(anyListOf(Urn.class), eq(0), any(PlaySessionSource.class));
        PlayerUICommand playerUICommand = eventBus.lastEventOn(EventQueue.PLAYER_COMMAND);
        assertThat(playerUICommand).isEqualTo(PlayerUICommand.showPlayer());
    }

    @Test
    public void shouldInsertIntoPlayQueue() {
        when(playQueueManager.isQueueEmpty()).thenReturn(false);
        when(playlistOperations.trackUrnsForPlayback(eq(playlistUrn)))
                .thenReturn(Observable.just(trackList));

        playQueueHelper.playNext(playlistUrn);

        verify(playQueueManager, times(1)).insertNext(anyListOf(Urn.class));
    }

}
