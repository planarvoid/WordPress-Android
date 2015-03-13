package com.soundcloud.android.sync.posts;

import static com.soundcloud.android.Expect.expect;
import static org.mockito.Mockito.when;

import com.soundcloud.android.api.model.ApiPlaylist;
import com.soundcloud.android.api.model.ApiTrack;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.sync.ApiSyncResult;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;

import android.net.Uri;

@RunWith(SoundCloudTestRunner.class)
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
        expect(result.change).toEqual(ApiSyncResult.CHANGED);
        expect(result.uri).toEqual(URI);
    }

    @Test
    public void returnsChangeResultIfPlaylistSyncChangedButTrackSyncDidNot() throws Exception {
        when(playlistPostsSyncer.call()).thenReturn(true);

        ApiSyncResult result = myPostsSyncer.syncContent(URI, null);
        expect(result.change).toEqual(ApiSyncResult.CHANGED);
        expect(result.uri).toEqual(URI);
    }

    @Test
    public void returnsUnchangedResultIfPlaylistsAndTracksDidNotChange() throws Exception {
        ApiSyncResult result = myPostsSyncer.syncContent(URI, null);
        expect(result.change).toEqual(ApiSyncResult.UNCHANGED);
        expect(result.uri).toEqual(URI);
    }
}