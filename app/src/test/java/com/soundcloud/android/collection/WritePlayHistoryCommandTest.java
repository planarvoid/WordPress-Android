package com.soundcloud.android.collection;

import com.soundcloud.android.collection.playhistory.PlayHistoryRecord;
import com.soundcloud.android.collection.playhistory.WritePlayHistoryCommand;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.testsupport.StorageIntegrationTest;
import org.junit.Before;
import org.junit.Test;

public class WritePlayHistoryCommandTest extends StorageIntegrationTest {

    private WritePlayHistoryCommand command;

    @Before
    public void setUp() {
        command = new WritePlayHistoryCommand(propeller());
    }

    @Test
    public void insertsNewPlayHistory() {
        PlayHistoryRecord record = PlayHistoryRecord.create(1000L, Urn.forTrack(123L), Urn.NOT_SET);

        command.call(record);

        databaseAssertions().assertPlayHistory(record);
    }

    @Test
    public void insertsMultipleTimesTheSamePlayHistoryWithDifferentTimestamp() {
        PlayHistoryRecord record = PlayHistoryRecord.create(1000L, Urn.forTrack(123L), Urn.NOT_SET);
        command.call(record);

        PlayHistoryRecord record2 = PlayHistoryRecord.create(2000L, Urn.forTrack(123L), Urn.NOT_SET);
        command.call(record2);

        databaseAssertions().assertPlayHistory(record);
        databaseAssertions().assertPlayHistory(record2);
    }

    @Test
    public void insertsOnlyOnceWhenTimestampAlreadyExistsForSameTrack() {
        PlayHistoryRecord record = PlayHistoryRecord.create(1000L, Urn.forTrack(123L), Urn.NOT_SET);
        command.call(record);
        command.call(record);

        databaseAssertions().assertPlayHistory(record, 1);
    }


}
