package com.soundcloud.android.offline.commands;

import com.soundcloud.android.model.Urn;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.testsupport.StorageIntegrationTest;
import com.soundcloud.propeller.PropellerWriteException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(SoundCloudTestRunner.class)
public class StoreOfflinePlaylistCommandTest extends StorageIntegrationTest {

    private StoreOfflinePlaylistCommand command;

    @Before
    public void setUp() {
        command = new StoreOfflinePlaylistCommand(propeller());
    }

    @Test
    public void storesPlaylistInOfflineContentTable() throws PropellerWriteException {
        final Urn playlistUrn = Urn.forPlaylist(123L);

        command.with(playlistUrn).call();

        databaseAssertions().assertPlaylistMarkedForOfflineSync(playlistUrn);
    }

}