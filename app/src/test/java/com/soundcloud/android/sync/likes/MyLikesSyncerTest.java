package com.soundcloud.android.sync.likes;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.api.model.ApiPlaylist;
import com.soundcloud.android.api.model.ApiTrack;
import com.soundcloud.android.sync.ApiSyncResult;
import com.soundcloud.android.testsupport.InjectionSupport;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import android.net.Uri;

@RunWith(MockitoJUnitRunner.class)
public class MyLikesSyncerTest {

    private static final Uri URI = Uri.parse("/some/uri");

    private MyLikesSyncer myLikesSyncer;

    @Mock private LikesSyncer<ApiTrack> trackLikesSyncer;
    @Mock private LikesSyncer<ApiPlaylist> playlistLikesSyncer;

    @Before
    public void setUp() throws Exception {
        myLikesSyncer = new MyLikesSyncer(
                InjectionSupport.lazyOf(trackLikesSyncer),
                InjectionSupport.lazyOf(playlistLikesSyncer));
    }

    @Test
    public void returnsChangeResultIfTrackSyncChangedButPlaylistSyncDidNot() throws Exception {
        when(trackLikesSyncer.call()).thenReturn(true);

        ApiSyncResult result = myLikesSyncer.syncContent(URI, null);
        assertThat(result.change).isEqualTo(ApiSyncResult.CHANGED);
        assertThat(result.uri).isEqualTo(URI);
    }

    @Test
    public void returnsChangeResultIfPlaylistSyncChangedButTrackSyncDidNot() throws Exception {
        when(playlistLikesSyncer.call()).thenReturn(true);

        ApiSyncResult result = myLikesSyncer.syncContent(URI, null);
        assertThat(result.change).isEqualTo(ApiSyncResult.CHANGED);
        assertThat(result.uri).isEqualTo(URI);
    }

    @Test
    public void returnsUnchangedResultIfPlaylistsAndTracksDidNotChange() throws Exception {
        ApiSyncResult result = myLikesSyncer.syncContent(URI, null);
        assertThat(result.change).isEqualTo(ApiSyncResult.UNCHANGED);
        assertThat(result.uri).isEqualTo(URI);
    }

    @Test
    public void callsSyncLikesAndSyncPlaylistEachTime() throws Exception {
        when(trackLikesSyncer.call()).thenReturn(true);
        when(playlistLikesSyncer.call()).thenReturn(true);

        myLikesSyncer.syncContent(URI, null);

        verify(trackLikesSyncer).call();
        verify(playlistLikesSyncer).call();
    }

    @Test
    public void callsSyncPlaylistAndSyncLikesEachTime() throws Exception {
        myLikesSyncer.syncContent(URI, null);

        verify(trackLikesSyncer).call();
        verify(playlistLikesSyncer).call();
    }
}