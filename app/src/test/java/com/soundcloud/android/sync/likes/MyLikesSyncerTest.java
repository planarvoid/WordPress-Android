package com.soundcloud.android.sync.likes;

import static com.soundcloud.android.Expect.expect;
import static org.mockito.Mockito.when;

import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.sync.ApiSyncResult;
import dagger.Lazy;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;

import android.net.Uri;

@RunWith(SoundCloudTestRunner.class)
public class MyLikesSyncerTest {

    private static final Uri URI = Uri.parse("/some/uri");

    private MyLikesSyncer myLikesSyncer;

    @Mock private LikesSyncer trackLikesSyncer;
    @Mock private LikesSyncer playlistLikesSyncer;

    @Before
    public void setUp() throws Exception {
        myLikesSyncer = new MyLikesSyncer(new Lazy<LikesSyncer>() {
            @Override
            public LikesSyncer get() {
                return trackLikesSyncer;
            }
        }, new Lazy<LikesSyncer>() {
            @Override
            public LikesSyncer get() {
                return playlistLikesSyncer;
            }
        });
    }

    @Test
    public void returnsChangeResultIfTrackSyncChangedButPlaylistSyncDidNot() throws Exception {
        when(trackLikesSyncer.call()).thenReturn(true);

        ApiSyncResult result = myLikesSyncer.syncContent(URI, null);
        expect(result.change).toEqual(ApiSyncResult.CHANGED);
        expect(result.uri).toEqual(URI);
    }

    @Test
    public void returnsChangeResultIfPlaylistSyncChangedButTrackSyncDidNot() throws Exception {
        when(playlistLikesSyncer.call()).thenReturn(true);

        ApiSyncResult result = myLikesSyncer.syncContent(URI, null);
        expect(result.change).toEqual(ApiSyncResult.CHANGED);
        expect(result.uri).toEqual(URI);
    }

    @Test
    public void returnsUnchangedResultIfPlaylistsAndTracksDidNotChange() throws Exception {
        ApiSyncResult result = myLikesSyncer.syncContent(URI, null);
        expect(result.change).toEqual(ApiSyncResult.UNCHANGED);
        expect(result.uri).toEqual(URI);
    }


}