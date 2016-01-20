package com.soundcloud.android.offline;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

import com.soundcloud.android.api.model.ApiPlaylist;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.testsupport.StorageIntegrationTest;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import java.util.List;

public class ClearTrackDownloadsCommandTest extends StorageIntegrationTest {

    private ClearTrackDownloadsCommand command;

    @Mock SecureFileStorage secureFileStorage;
    @Mock OfflineContentStorage offlineContentStorage;
    @Mock TrackOfflineStateProvider trackOfflineStateProvider;

    @Before
    public void setUp() throws Exception {
        command = new ClearTrackDownloadsCommand(propeller(), secureFileStorage, offlineContentStorage, trackOfflineStateProvider);
    }

    @Test
    public void removesTrackDownloads() {
        Urn track = Urn.forTrack(123L);
        testFixtures().insertTrackPendingDownload(track, 123L);

        List<Urn> removed = command.call(null);

        databaseAssertions().assertNotDownloaded(track);
        assertThat(removed).containsExactly(track);
    }

    @Test
    public void removesLikesFromOfflineContent() {
        testFixtures().insertLikesMarkedForOfflineSync();

        List<Urn> removed = command.call(null);
        databaseAssertions().assertOfflineLikesDisabled();
        assertThat(removed).isEmpty();
    }

    @Test
    public void removesPlaylistsFromOfflineContent() {
        ApiPlaylist playlist = testFixtures().insertPlaylistMarkedForOfflineSync();

        List<Urn> removed = command.call(null);

        databaseAssertions().assertPlaylistNotMarkedForOfflineSync(playlist.getUrn());
        assertThat(removed).containsExactly(playlist.getUrn());
    }

    @Test
    public void clearsTrackOfflineState() {
        command.call(null);

        verify(trackOfflineStateProvider).clear();
    }

    @Test
    public void clearOfflineContentRemovesOfflineTrackFiles() {
        command.call(null);

        verify(secureFileStorage).deleteAllTracks();
    }

    @Test
    public void clearOfflineContentClearsOfflineContentState() {
        command.call(null);

        verify(offlineContentStorage).setHasOfflineContent(false);
    }

}
