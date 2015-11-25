package com.soundcloud.android.sync.playlists;

import com.soundcloud.android.api.model.ApiPlaylist;
import com.soundcloud.android.testsupport.StorageIntegrationTest;
import com.soundcloud.android.testsupport.fixtures.ModelFixtures;
import org.junit.Before;
import org.junit.Test;

public class StorePlaylistCommandTest extends StorageIntegrationTest {

    private StorePlaylistCommand command;

    @Before
    public void setup() {
        command = new StorePlaylistCommand(propeller());
    }

    @Test
    public void shouldPersistPlaylistWithUserInDatabase() throws Exception {
        final ApiPlaylist input = ModelFixtures.create(ApiPlaylist.class);

        command.with(input).call();

        databaseAssertions().assertPlaylistWithUserInserted(input);
    }
}
