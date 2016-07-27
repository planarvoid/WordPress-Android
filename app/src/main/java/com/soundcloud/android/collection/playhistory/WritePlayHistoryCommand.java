package com.soundcloud.android.collection.playhistory;

import com.soundcloud.android.commands.DefaultWriteStorageCommand;
import com.soundcloud.android.storage.Tables;
import com.soundcloud.propeller.ContentValuesBuilder;
import com.soundcloud.propeller.PropellerDatabase;
import com.soundcloud.propeller.WriteResult;

import android.content.ContentValues;

import javax.inject.Inject;

public class WritePlayHistoryCommand extends DefaultWriteStorageCommand<PlayHistoryRecord, WriteResult> {

    @Inject
    public WritePlayHistoryCommand(PropellerDatabase database) {
        super(database);
    }

    @Override
    protected WriteResult write(PropellerDatabase propeller, PlayHistoryRecord input) {
        return propeller.upsert(Tables.PlayHistory.TABLE, buildContentValue(input));
    }

    private ContentValues buildContentValue(PlayHistoryRecord record) {
        return ContentValuesBuilder.values()
                                   .put(Tables.PlayHistory.TIMESTAMP, record.timestamp())
                                   .put(Tables.PlayHistory.TRACK_ID, record.trackUrn().getNumericId())
                                   .get();
    }
}
