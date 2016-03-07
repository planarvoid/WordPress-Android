package com.soundcloud.android.sync.playlists;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.soundcloud.android.model.Urn;
import com.soundcloud.android.sync.ApiSyncResult;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import android.net.Uri;

@RunWith(MockitoJUnitRunner.class)
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
        assertThat(result.change).isEqualTo(ApiSyncResult.CHANGED);
        assertThat(result.uri).isEqualTo(URI);
    }

    @Test
    public void returnsUnchangedResultIfPlaylistSyncDidNotChangeAnything() throws Exception {
        ApiSyncResult result = legacySinglePlaylistSyncer.syncContent(URI, null);
        assertThat(result.change).isEqualTo(ApiSyncResult.UNCHANGED);
        assertThat(result.uri).isEqualTo(URI);
    }
}
