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
        PlayHistoryRecord record = PlayHistoryRecord.create(1000L, Urn.forTrack(123L), Urn.forArtistStation(234L));

        command.call(record);

        databaseAssertions().assertPlayHistory(record);
    }

    @Test
    public void updatesTimestampForPlayHistoryWithSameTrackAndContext() {
        PlayHistoryRecord record = PlayHistoryRecord.create(1000L, Urn.forTrack(123L), Urn.forArtistStation(234L));
        command.call(record);

        PlayHistoryRecord record2 = PlayHistoryRecord.create(2000L, Urn.forTrack(123L), Urn.forArtistStation(234L));
        command.call(record2);

        databaseAssertions().assertNoPlayHistory(record);
        databaseAssertions().assertPlayHistory(record2);
    }

    @Test
    public void insertsMultiplePlayHistoryForDifferentContextsAndSameTrack() {
        PlayHistoryRecord record = PlayHistoryRecord.create(1000L, Urn.forTrack(123L), Urn.forArtistStation(1L));
        command.call(record);

        PlayHistoryRecord record2 = PlayHistoryRecord.create(1000L, Urn.forTrack(123L), Urn.forArtistStation(2L));
        command.call(record2);

        databaseAssertions().assertPlayHistory(record);
        databaseAssertions().assertPlayHistory(record2);
    }

}
