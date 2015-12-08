package com.soundcloud.android.playlists;

import static android.provider.BaseColumns._ID;
import static com.soundcloud.android.storage.Table.Sounds;
import static com.soundcloud.android.storage.TableColumns.ResourceTable._TYPE;
import static com.soundcloud.android.storage.TableColumns.Sounds.TYPE_PLAYLIST;
import static com.soundcloud.propeller.query.Query.from;
import static com.soundcloud.propeller.test.assertions.QueryAssertions.assertThat;

import com.soundcloud.android.api.model.ApiPlaylist;
import com.soundcloud.android.testsupport.StorageIntegrationTest;
import com.soundcloud.propeller.query.Query;
import org.junit.Before;
import org.junit.Test;

public class RemovePlaylistCommandTest extends StorageIntegrationTest {

    private RemovePlaylistCommand command;

    @Before
    public void setup() {
        command = new RemovePlaylistCommand(propeller());
    }

    @Test
    public void removesPlaylist() throws Exception {
        testFixtures().insertPlaylist();
        final ApiPlaylist playlist = testFixtures().insertPlaylist();

        command.call(playlist.getUrn());

        final Query query = from(Sounds.name())
                .whereEq(_ID, playlist.getId())
                .whereEq(_TYPE, TYPE_PLAYLIST);

        assertThat(select(query)).isEmpty();
    }

}
