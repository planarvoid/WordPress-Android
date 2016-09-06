package com.soundcloud.android.collection.playhistory;

import com.soundcloud.android.model.Urn;
import com.soundcloud.android.testsupport.StorageIntegrationTest;
import org.junit.Before;
import org.junit.Test;

public class OptimizePlayHistoryCommandTest extends StorageIntegrationTest {

    private OptimizePlayHistoryCommand command;

    @Before
    public void setUp() throws Exception {
        command = new OptimizePlayHistoryCommand(propeller());
    }

    @Test
    public void shouldKeepLastRows() {
        Urn urn = Urn.forTrack(123L);

        testFixtures().insertPlayHistory(1000L, urn);
        testFixtures().insertPlayHistory(3000L, urn);
        testFixtures().insertPlayHistory(4000L, urn);
        testFixtures().insertPlayHistory(5000L, urn);
        testFixtures().insertPlayHistory(2000L, urn);

        command.call(3);

        databaseAssertions().assertPlayHistoryCount(3);
        databaseAssertions().assertPlayHistory(PlayHistoryRecord.create(3000L, urn, Urn.NOT_SET));
        databaseAssertions().assertPlayHistory(PlayHistoryRecord.create(4000L, urn, Urn.NOT_SET));
        databaseAssertions().assertPlayHistory(PlayHistoryRecord.create(5000L, urn, Urn.NOT_SET));
    }

    @Test
    public void shouldNotDeleteAnythingWhenAlreadyBelowLimit() {
        Urn urn = Urn.forTrack(123L);

        testFixtures().insertPlayHistory(1000L, urn);
        testFixtures().insertPlayHistory(3000L, urn);

        command.call(3);

        databaseAssertions().assertPlayHistoryCount(2);
        databaseAssertions().assertPlayHistory(PlayHistoryRecord.create(1000L, urn, Urn.NOT_SET));
        databaseAssertions().assertPlayHistory(PlayHistoryRecord.create(3000L, urn, Urn.NOT_SET));
    }

}
