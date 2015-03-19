package com.soundcloud.android.sync.playlists;

import static com.soundcloud.android.Expect.expect;
import static org.mockito.Mockito.when;

import com.soundcloud.android.model.Urn;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.sync.ApiSyncResult;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;

import android.net.Uri;

@RunWith(SoundCloudTestRunner.class)
public class LegacySinglePlaylistSyncerTest {

    private static final Uri URI = Uri.parse("/some/uri");
    private static final Urn PLAYLIST_URN = Urn.forPlaylist(123L);

    private LegacySinglePlaylistSyncer legacySinglePlaylistSyncer;

    @Mock private SinglePlaylistSyncerFactory singlePlaylistSyncerFactory;
    @Mock private SinglePlaylistSyncer singlePlaylistSyncer;

    @Before
    public void setUp() throws Exception {
        when(singlePlaylistSyncerFactory.create(PLAYLIST_URN)).thenReturn(singlePlaylistSyncer);
        legacySinglePlaylistSyncer = new LegacySinglePlaylistSyncer(singlePlaylistSyncerFactory, PLAYLIST_URN);
    }

    @Test
    public void returnsChangeResultIfPlaylistSyncChangedSomething() throws Exception {
        when(singlePlaylistSyncer.call()).thenReturn(true);

        ApiSyncResult result = legacySinglePlaylistSyncer.syncContent(URI, null);
        expect(result.change).toEqual(ApiSyncResult.CHANGED);
        expect(result.uri).toEqual(URI);
    }

    @Test
    public void returnsUnchangedResultIfPlaylistSyncDidNotChangeAnything() throws Exception {
        ApiSyncResult result = legacySinglePlaylistSyncer.syncContent(URI, null);
        expect(result.change).toEqual(ApiSyncResult.UNCHANGED);
        expect(result.uri).toEqual(URI);
    }
}