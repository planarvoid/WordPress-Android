package com.soundcloud.android.commands;

import static android.provider.BaseColumns._ID;
import static com.soundcloud.android.storage.Tables.Sounds.DESCRIPTION;
import static com.soundcloud.android.testsupport.fixtures.ModelFixtures.create;
import static com.soundcloud.propeller.query.Query.from;
import static com.soundcloud.propeller.test.assertions.QueryAssertions.assertThat;
import static java.util.Collections.singletonList;

import com.soundcloud.android.api.model.ApiTrack;
import com.soundcloud.android.storage.Tables.Sounds;
import com.soundcloud.android.testsupport.StorageIntegrationTest;
import com.soundcloud.android.testsupport.fixtures.ModelFixtures;
import com.soundcloud.propeller.PropellerWriteException;
import org.assertj.core.api.Java6Assertions;
import org.junit.Before;
import org.junit.Test;

import java.util.List;

public class StoreTracksCommandTest extends StorageIntegrationTest {

    private StoreTracksCommand command;

    @Before
    public void setup() {
        command = new StoreTracksCommand(propeller(), new StoreUsersCommand(propeller()));
    }

    @Test
    public void shouldPersistTracksWithCreatorsInDatabase() throws Exception {
        final List<ApiTrack> tracks = ModelFixtures.create(ApiTrack.class, 2);
        command.call(tracks);
        databaseAssertions().assertTrackWithUserInserted(tracks.get(0));
        databaseAssertions().assertTrackWithUserInserted(tracks.get(1));
    }

    @Test
    public void shouldStoreTracksUsingUpsert() throws Exception {
        final ApiTrack track = testFixtures().insertTrack();
        track.setTitle("new title");

        command.call(singletonList(track));

        assertThat(select(from(Sounds.TABLE))).counts(1);
        databaseAssertions().assertTrackInserted(track);
    }

    @Test
    public void shouldStoreBlockedTrack() throws Exception {
        final ApiTrack track = testFixtures().insertTrack();
        track.setBlocked(true);

        command.call(singletonList(track));

        assertThat(select(from(Sounds.TABLE))).counts(1);
        databaseAssertions().assertTrackInserted(track);
    }

    @Test
    public void shouldStoreSnippedTrack() {
        final ApiTrack track = testFixtures().insertTrack();
        track.setSnipped(true);

        command.call(singletonList(track));

        assertThat(select(from(Sounds.TABLE))).counts(1);
        databaseAssertions().assertTrackInserted(track);
    }

    @Test
    public void shouldPersistTrackWithDescription() {
        ApiTrack track = create(ApiTrack.class);
        track.setDescription("description");

        command.call(singletonList(track));

        assertThat(select(from(Sounds.TABLE)
                                  .whereEq(_ID, track.getId())
                                  .whereEq(DESCRIPTION, "description"))).counts(1);

    }

    @Test
    public void failsToStoreTrackWithoutPolicy() {
        ApiTrack track = create(ApiTrack.class);
        track.setPolicy(null);
        try {
            command.call(singletonList(track));
        } catch (PropellerWriteException exception) {
            Java6Assertions.assertThat(exception.getCause().getMessage()).contains("Track policy should not be null");
            return;
        }
        Java6Assertions.fail("Should have failed with an IllegalStateException");
    }
}
