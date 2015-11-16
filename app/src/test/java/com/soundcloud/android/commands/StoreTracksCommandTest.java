package com.soundcloud.android.commands;

import static com.soundcloud.android.storage.Table.Sounds;
import static com.soundcloud.android.storage.TableColumns.Sounds.DESCRIPTION;
import static com.soundcloud.android.testsupport.fixtures.ModelFixtures.create;
import static com.soundcloud.propeller.query.Query.from;
import static com.soundcloud.propeller.test.assertions.QueryAssertions.assertThat;
import static java.util.Collections.singletonList;

import com.soundcloud.android.api.legacy.model.PublicApiTrack;
import com.soundcloud.android.api.model.ApiTrack;
import com.soundcloud.android.testsupport.StorageIntegrationTest;
import com.soundcloud.android.testsupport.fixtures.ModelFixtures;
import org.junit.Before;
import org.junit.Test;

import java.util.List;

public class StoreTracksCommandTest extends StorageIntegrationTest {

    private StoreTracksCommand command;

    @Before
    public void setup() {
        command = new StoreTracksCommand(propeller());
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

        assertThat(select(from(Sounds.name()))).counts(1);
        databaseAssertions().assertTrackInserted(track);
    }

    @Test
    public void shouldPersistTrackWithDescription() {
        PublicApiTrack track = create(PublicApiTrack.class);
        track.description = "description";

        command.call(singletonList(track));

        assertThat(select(from(Sounds.name())
                .whereEq(DESCRIPTION, "description"))).counts(1);

    }
}
