package com.soundcloud.android.collection.recentlyplayed;

import com.soundcloud.android.collection.playhistory.PlayHistoryRecord;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.testsupport.StorageIntegrationTest;
import org.junit.Before;
import org.junit.Test;

public class OptimizeRecentlyPlayedCommandTest extends StorageIntegrationTest {

    private OptimizeRecentlyPlayedCommand command;

    @Before
    public void setUp() throws Exception {
        command = new OptimizeRecentlyPlayedCommand(propeller());
    }

    @Test
    public void shouldKeepLastRows() {
        Urn urn = Urn.forPlaylist(123L);

        testFixtures().insertRecentlyPlayed(1000L, urn);
        testFixtures().insertRecentlyPlayed(3000L, urn);
        testFixtures().insertRecentlyPlayed(4000L, urn);
        testFixtures().insertRecentlyPlayed(5000L, urn);
        testFixtures().insertRecentlyPlayed(2000L, urn);

        command.call(3);

        databaseAssertions().assertRecentlyPlayedCount(3);
        databaseAssertions().assertRecentlyPlayed(PlayHistoryRecord.create(3000L, Urn.NOT_SET, urn));
        databaseAssertions().assertRecentlyPlayed(PlayHistoryRecord.create(4000L, Urn.NOT_SET, urn));
        databaseAssertions().assertRecentlyPlayed(PlayHistoryRecord.create(5000L, Urn.NOT_SET, urn));
    }

    @Test
    public void shouldNotDeleteAnythingWhenAlreadyBelowLimit() {
        Urn urn = Urn.forPlaylist(123L);

        testFixtures().insertRecentlyPlayed(1000L, urn);
        testFixtures().insertRecentlyPlayed(3000L, urn);

        command.call(3);

        databaseAssertions().assertRecentlyPlayedCount(2);
        databaseAssertions().assertRecentlyPlayed(PlayHistoryRecord.create(1000L, Urn.NOT_SET, urn));
        databaseAssertions().assertRecentlyPlayed(PlayHistoryRecord.create(3000L, Urn.NOT_SET, urn));
    }

}
