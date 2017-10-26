package com.soundcloud.android.commands;

import static android.provider.BaseColumns._ID;
import static com.soundcloud.android.storage.Tables.Sounds.DESCRIPTION;
import static com.soundcloud.propeller.query.Query.from;
import static com.soundcloud.propeller.test.assertions.QueryAssertions.assertThat;
import static java.util.Collections.singletonList;

import com.soundcloud.android.api.model.ApiTrack;
import com.soundcloud.android.storage.Tables.Sounds;
import com.soundcloud.android.testsupport.StorageIntegrationTest;
import com.soundcloud.android.testsupport.TrackFixtures;
import com.soundcloud.java.optional.Optional;
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
        final List<ApiTrack> tracks = TrackFixtures.apiTracks(2);
        command.call(tracks);
        databaseAssertions().assertTrackWithUserInserted(tracks.get(0));
        databaseAssertions().assertTrackWithUserInserted(tracks.get(1));
    }

    @Test
    public void shouldStoreTracksUsingUpsert() throws Exception {
        ApiTrack updatedTrack = testFixtures().insertTrack().toBuilder().title("new title").build();

        command.call(singletonList(updatedTrack));

        assertThat(select(from(Sounds.TABLE))).counts(1);
        databaseAssertions().assertTrackInserted(updatedTrack);
    }

    @Test
    public void shouldStoreBlockedTrack() throws Exception {
        ApiTrack updatedTrack = testFixtures().insertTrack().toBuilder().blocked(true).build();

        command.call(singletonList(updatedTrack));

        assertThat(select(from(Sounds.TABLE))).counts(1);
        databaseAssertions().assertTrackInserted(updatedTrack);
    }

    @Test
    public void shouldStoreSnippedTrack() {
        final ApiTrack originalTrack = testFixtures().insertTrack();
        ApiTrack updatedTrack = originalTrack.toBuilder().snipped(true).build();

        command.call(singletonList(updatedTrack));

        assertThat(select(from(Sounds.TABLE))).counts(1);
        databaseAssertions().assertTrackInserted(updatedTrack);
    }

    @Test
    public void shouldPersistTrackWithDescription() {
        ApiTrack track = TrackFixtures.apiTrackBuilder().description(Optional.of("description")).build();

        command.call(singletonList(track));

        assertThat(select(from(Sounds.TABLE)
                                  .whereEq(_ID, track.getId())
                                  .whereEq(DESCRIPTION, "description"))).counts(1);

    }
}
