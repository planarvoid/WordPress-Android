package com.soundcloud.android.sync.playlists;

import static com.soundcloud.android.testsupport.InjectionSupport.providerOf;

import com.soundcloud.android.api.model.ApiPlaylist;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.testsupport.StorageIntegrationTest;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;

@RunWith(SoundCloudTestRunner.class)
public class RemoveLocalPlaylistsCommandTest extends StorageIntegrationTest {

    @Mock private Thread backgroundThread;

    private RemoveLocalPlaylistsCommand command;

    @Before
    public void setUp() throws Exception {
        command = new RemoveLocalPlaylistsCommand(propeller(), providerOf(backgroundThread));
    }

    @Test
    public void removesLocalPlayists() throws Exception {
        ApiPlaylist remotePlaylist = testFixtures().insertPlaylist();
        ApiPlaylist localPlaylist = testFixtures().insertLocalPlaylist();

        command.call(null);

        databaseAssertions().assertPlaylistInserted(remotePlaylist.getId());
        databaseAssertions().assertPlaylistNotStored(localPlaylist);
    }
}