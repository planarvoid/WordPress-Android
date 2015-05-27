package com.soundcloud.android.offline.commands;

import static com.soundcloud.android.Expect.expect;
import static org.mockito.Mockito.verify;

import com.soundcloud.android.api.model.ApiPlaylist;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.offline.SecureFileStorage;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.testsupport.StorageIntegrationTest;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;

import java.util.List;

@RunWith(SoundCloudTestRunner.class)
public class ClearTrackDownloadsCommandTest extends StorageIntegrationTest {

    private ClearTrackDownloadsCommand command;

    @Mock SecureFileStorage secureFileStorage;

    @Before
    public void setUp() throws Exception {
        command = new ClearTrackDownloadsCommand(propeller(), secureFileStorage);
    }

    @Test
    public void removesTrackDownloads() {
        Urn track = Urn.forTrack(123L);
        testFixtures().insertTrackPendingDownload(track, 123L);

        List<Urn> removed = command.call(null);

        databaseAssertions().assertTrackDownloadNotStored(track);
        expect(removed).toContainExactly(track);
    }

    @Test
    public void removesOfflineContent() {
        ApiPlaylist playlist = testFixtures().insertPlaylistMarkedForOfflineSync();

        List<Urn> removed = command.call(null);

        databaseAssertions().assertPlaylistNotMarkedForOfflineSync(playlist.getUrn());
        expect(removed).toContainExactly(playlist.getUrn());
    }

    @Test
    public void clearOfflineContentRemovesOfflineTrackFiles() {
        command.call(null);

        verify(secureFileStorage).deleteAllTracks();
    }

}
