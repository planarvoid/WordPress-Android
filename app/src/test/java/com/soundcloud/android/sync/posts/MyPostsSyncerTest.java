package com.soundcloud.android.sync.posts;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.soundcloud.android.api.model.ApiPlaylist;
import com.soundcloud.android.api.model.ApiTrack;
import com.soundcloud.android.sync.ApiSyncResult;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import android.net.Uri;

@RunWith(MockitoJUnitRunner.class)
public class MyPostsSyncerTest {

    private static final Uri URI = Uri.parse("/some/uri");

    private MyPostsSyncer myPostsSyncer;

    @Mock private PostsSyncer<ApiTrack> trackPostsSyncer;
    @Mock private PostsSyncer<ApiPlaylist> playlistPostsSyncer;

    @Before
    public void setUp() throws Exception {
        myPostsSyncer = new MyPostsSyncer(trackPostsSyncer, playlistPostsSyncer);
    }

    @Test
    public void returnsChangeResultIfTrackSyncChangedButPlaylistSyncDidNot() throws Exception {
        when(trackPostsSyncer.call()).thenReturn(true);

        ApiSyncResult result = myPostsSyncer.syncContent(URI, null);
        assertThat(result.change).isEqualTo(ApiSyncResult.CHANGED);
        assertThat(result.uri).isEqualTo(URI);
    }

    @Test
    public void returnsChangeResultIfPlaylistSyncChangedButTrackSyncDidNot() throws Exception {
        when(playlistPostsSyncer.call()).thenReturn(true);

        ApiSyncResult result = myPostsSyncer.syncContent(URI, null);
        assertThat(result.change).isEqualTo(ApiSyncResult.CHANGED);
        assertThat(result.uri).isEqualTo(URI);
    }

    @Test
    public void returnsUnchangedResultIfPlaylistsAndTracksDidNotChange() throws Exception {
        ApiSyncResult result = myPostsSyncer.syncContent(URI, null);
        assertThat(result.change).isEqualTo(ApiSyncResult.UNCHANGED);
        assertThat(result.uri).isEqualTo(URI);
    }
}
