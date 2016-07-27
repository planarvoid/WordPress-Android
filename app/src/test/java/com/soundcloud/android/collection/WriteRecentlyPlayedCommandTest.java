package com.soundcloud.android.collection;

import com.soundcloud.android.collection.playhistory.PlayHistoryRecord;
import com.soundcloud.android.collection.recentlyplayed.WriteRecentlyPlayedCommand;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.testsupport.StorageIntegrationTest;
import org.junit.Before;
import org.junit.Test;

public class WriteRecentlyPlayedCommandTest extends StorageIntegrationTest {

    private WriteRecentlyPlayedCommand command;

    @Before
    public void setUp() {
        command = new WriteRecentlyPlayedCommand(propeller());
    }

    @Test
    public void insertsNewRecentlyPlayed() {
        PlayHistoryRecord record = PlayHistoryRecord.create(1000L, Urn.forTrack(123L), Urn.forArtistStation(1L));

        command.call(record);

        databaseAssertions().assertRecentlyPlayed(record, 1);
    }

    @Test
    public void insertsMultipleTimesTheSameRecentlyPlayedWithDifferentTimestamp() {
        PlayHistoryRecord record = PlayHistoryRecord.create(1000L, Urn.forTrack(123L), Urn.forArtistStation(1L));
        command.call(record);

        PlayHistoryRecord record2 = PlayHistoryRecord.create(2000L, Urn.forTrack(123L), Urn.forArtistStation(1L));
        command.call(record2);

        databaseAssertions().assertRecentlyPlayed(record, 1);
        databaseAssertions().assertRecentlyPlayed(record2, 1);
    }

    @Test
    public void insertsOnlyOnceWhenTimestampAlreadyExistsForSameContext() {
        PlayHistoryRecord record = PlayHistoryRecord.create(1000L, Urn.forTrack(123L), Urn.forArtistStation(1L));
        command.call(record);
        command.call(record);

        databaseAssertions().assertRecentlyPlayed(record, 1);
    }

}
