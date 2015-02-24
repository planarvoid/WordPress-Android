package com.soundcloud.android.sync.playlists;

import static com.soundcloud.propeller.test.matchers.QueryMatchers.counts;
import static org.junit.Assert.assertThat;

import com.soundcloud.android.api.model.ApiPlaylist;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.storage.Table;
import com.soundcloud.android.storage.TableColumns;
import com.soundcloud.android.testsupport.StorageIntegrationTest;
import com.soundcloud.propeller.query.Query;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(SoundCloudTestRunner.class)
public class RemovePlaylistCommandTest  extends StorageIntegrationTest {

    private RemovePlaylistCommand command;

    @Before
    public void setup() {
        command = new RemovePlaylistCommand(propeller());
    }

    @Test
    public void removesPlaylist() throws Exception {
        testFixtures().insertPlaylist();
        final ApiPlaylist playlist = testFixtures().insertPlaylist();

        command.with(playlist.getUrn()).call();

        final Query query = Query.from(Table.Sounds.name())
                .whereEq(TableColumns.Sounds._ID, playlist.getId())
                .whereEq(TableColumns.Sounds._TYPE, TableColumns.Sounds.TYPE_PLAYLIST);

        assertThat(select(query), counts(0));
    }

}