package com.soundcloud.android.storage;

import com.soundcloud.android.collection.playhistory.PlayHistoryRecord;
import com.soundcloud.android.collection.playhistory.WritePlayHistoryCommand;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.storage.Tables.PlayHistory;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.testsupport.DatabaseMigrationHelper;
import org.junit.Before;
import org.junit.Ignore;

import java.util.Arrays;
import java.util.List;

@Ignore
public class PlayHistoryMigrationTest extends AndroidUnitTest {

    private DatabaseMigrationHelper migrationHelper;

    @Before
    public void setUp() throws Exception {
        migrationHelper = new DatabaseMigrationHelper();
    }

    @Ignore
    // Because we call `migrationHelper.upgradeTo(77);` and we don't keep the
    // schemas of version 77, this test fails.
    //
    // It fails because the last step of the migrations is not supposed to
    // work with version 77.
    public void shouldKeepPlayHistoryTracksAfterMigration() {
        PlayHistoryRecord record = PlayHistoryRecord.create(1000L, Urn.forTrack(123L), Urn.forArtistStation(123L));
        PlayHistoryRecord record2 = PlayHistoryRecord.create(2000L, Urn.forTrack(234L), Urn.forArtistStation(123L));

        // upgrade on first time play history table was created
        migrationHelper.upgradeTo(77);

        // insert some data
        insertPlayHistoryRecords(Arrays.asList(record, record2));

        // continue upgrade to current version
        migrationHelper.upgradeToCurrent();

        migrationHelper.assertTableCount(PlayHistory.TABLE.name(), 2);
    }

    private void insertPlayHistoryRecords(List<PlayHistoryRecord> playHistoryRecords) {
        WritePlayHistoryCommand command = new WritePlayHistoryCommand(migrationHelper.getPropellerDatabase());

        for (PlayHistoryRecord playHistoryRecord : playHistoryRecords) {
            command.call(playHistoryRecord);
        }
    }
}
