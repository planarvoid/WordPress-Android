package com.soundcloud.android.offline.commands;

import com.soundcloud.android.model.Urn;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.testsupport.StorageIntegrationTest;
import com.soundcloud.propeller.PropellerWriteException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(SoundCloudTestRunner.class)
public class RemoveOfflinePlaylistCommandTest extends StorageIntegrationTest {

    private RemoveOfflinePlaylistCommand command;

    @Before
    public void setUp() {
        command = new RemoveOfflinePlaylistCommand(propeller());
    }

    @Test
    public void removesPlaylistInOfflineContentTable() throws PropellerWriteException {
        final Urn playlistUrn = testFixtures().insertPlaylistMarkedForOfflineSync();

        command.with(playlistUrn).call();

        databaseAssertions().assertPlaylistNotMarkedForOfflineSync(playlistUrn);
    }

}