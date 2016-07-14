package com.soundcloud.android.collection.playhistory;

import com.soundcloud.android.collection.playhistory.PlayHistoryRecord;
import com.soundcloud.android.commands.Command;
import com.soundcloud.android.storage.Tables;
import com.soundcloud.propeller.ContentValuesBuilder;
import com.soundcloud.propeller.PropellerDatabase;

import android.content.ContentValues;

import javax.inject.Inject;

public class WritePlayHistoryCommand extends Command<PlayHistoryRecord, Boolean> {

    private final PropellerDatabase propeller;

    @Inject
    public WritePlayHistoryCommand(PropellerDatabase propeller) {
        this.propeller = propeller;
    }

    @Override
    public Boolean call(PlayHistoryRecord input) {
        return propeller.upsert(Tables.PlayHistory.TABLE, buildContentValue(input)).success();
    }

    private ContentValues buildContentValue(PlayHistoryRecord record) {
        return ContentValuesBuilder.values()
                                   .put(Tables.PlayHistory.TIMESTAMP, record.timestamp())
                                   .put(Tables.PlayHistory.TRACK_ID, record.trackUrn().getNumericId())
                                   .put(Tables.PlayHistory.CONTEXT_TYPE, record.getContextType())
                                   .put(Tables.PlayHistory.CONTEXT_ID, record.contextUrn().getNumericId())
                                   .get();
    }
}
