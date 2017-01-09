package com.soundcloud.android.playlists;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.PlaylistTrackCountChangedEvent;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.rx.eventbus.TestEventBus;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import android.support.v4.app.FragmentActivity;

public class PlaylistContentPresenterTest extends AndroidUnitTest {
    private static final Urn PLAYLIST_URN = Urn.forPlaylist(123L);

    @Mock private PlaylistPresenter playlistPresenter;
    @Mock private FragmentActivity activity;
    private TestEventBus eventBus;

    @Before
    public void setUp() throws Exception {
        eventBus = new TestEventBus();
    }

    @Test
    public void defaultViewReloadPlaylist() {
        final PlaylistDefaultView playlistDefaultView = new PlaylistDefaultView(eventBus, playlistPresenter);
        playlistDefaultView.start();

        eventBus.publish(EventQueue.PLAYLIST_CHANGED,
                         PlaylistTrackCountChangedEvent.fromTrackAddedToPlaylist(PLAYLIST_URN, 3));

        verify(playlistPresenter).reloadPlaylist();
    }


    @Test
    public void defaultViewDontReloadPlaylistWhenStopped() {
        final PlaylistDefaultView playlistDefaultView = new PlaylistDefaultView(eventBus, playlistPresenter);
        playlistDefaultView.start();
        playlistDefaultView.stop();

        eventBus.publish(EventQueue.PLAYLIST_CHANGED,
                         PlaylistTrackCountChangedEvent.fromTrackAddedToPlaylist(PLAYLIST_URN, 3));

        verify(playlistPresenter, never()).reloadPlaylist();
    }

    @Test
    public void editViewDontReloadPlaylist() {
        final PlaylistEditView playlistDefaultView = new PlaylistEditView(eventBus, playlistPresenter);
        playlistDefaultView.start();

        eventBus.publish(EventQueue.PLAYLIST_CHANGED,
                         PlaylistTrackCountChangedEvent.fromTrackAddedToPlaylist(PLAYLIST_URN, 3));

        verify(playlistPresenter, never()).reloadPlaylist();
    }

    @Test
    public void onStopEditModeSavePlaylist() {
        final PlaylistEditView playlistDefaultView = new PlaylistEditView(eventBus, playlistPresenter);
        playlistDefaultView.start();
        playlistDefaultView.stop();

        verify(playlistPresenter).savePlaylist();
    }
}
