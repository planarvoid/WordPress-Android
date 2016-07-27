package com.soundcloud.android.collection.recentlyplayed;

import com.soundcloud.android.collection.playhistory.PlayHistoryRecord;
import com.soundcloud.android.commands.DefaultWriteStorageCommand;
import com.soundcloud.android.storage.Tables;
import com.soundcloud.propeller.ContentValuesBuilder;
import com.soundcloud.propeller.PropellerDatabase;
import com.soundcloud.propeller.WriteResult;

import android.content.ContentValues;

import javax.inject.Inject;

public class WriteRecentlyPlayedCommand extends DefaultWriteStorageCommand<PlayHistoryRecord, WriteResult> {

    @Inject
    public WriteRecentlyPlayedCommand(PropellerDatabase propeller) {
        super(propeller);
    }

    @Override
    protected WriteResult write(PropellerDatabase propeller, PlayHistoryRecord input) {
        return propeller.upsert(Tables.RecentlyPlayed.TABLE, buildContentValue(input));
    }

    private ContentValues buildContentValue(PlayHistoryRecord record) {
        return ContentValuesBuilder.values()
                                   .put(Tables.RecentlyPlayed.TIMESTAMP, record.timestamp())
                                   .put(Tables.RecentlyPlayed.CONTEXT_TYPE, record.getContextType())
                                   .put(Tables.RecentlyPlayed.CONTEXT_ID, record.contextUrn().getNumericId())
                                   .get();
    }
}
