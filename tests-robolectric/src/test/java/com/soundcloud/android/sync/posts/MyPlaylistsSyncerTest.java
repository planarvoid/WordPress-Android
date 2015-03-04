package com.soundcloud.android.sync.posts;

import static com.soundcloud.android.Expect.expect;
import static org.mockito.Mockito.when;

import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.sync.ApiSyncResult;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;

import android.net.Uri;

@RunWith(SoundCloudTestRunner.class)
public class MyPlaylistsSyncerTest {

    private static final Uri URI = Uri.parse("/some/uri");

    private MyPlaylistsSyncer syncer;

    @Mock private PostsSyncer postsSyncer;

    @Before
    public void setUp() throws Exception {
        syncer = new MyPlaylistsSyncer(postsSyncer);
    }

    @Test
    public void shouldReturnChangedResultIfPostsSyncerReturnsTrue() throws Exception {
        when(postsSyncer.call()).thenReturn(true);
        final ApiSyncResult syncResult = syncer.syncContent(URI, null);
        expect(syncResult.change).toEqual(ApiSyncResult.CHANGED);
        expect(syncResult.uri).toEqual(URI);
    }

    @Test
    public void shouldReturnUnchangedResultIfPostsSyncerReturnsTrue() throws Exception {
        when(postsSyncer.call()).thenReturn(false);
        final ApiSyncResult syncResult = syncer.syncContent(URI, null);
        expect(syncResult.change).toEqual(ApiSyncResult.UNCHANGED);
        expect(syncResult.uri).toEqual(URI);
    }
}
