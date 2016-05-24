package com.soundcloud.android.collection;

import com.soundcloud.android.commands.WriteStorageCommand;
import com.soundcloud.android.storage.Tables;
import com.soundcloud.propeller.ChangeResult;
import com.soundcloud.propeller.ContentValuesBuilder;
import com.soundcloud.propeller.PropellerDatabase;

import android.content.ContentValues;

import javax.inject.Inject;

public class WritePlayHistoryCommand
        extends WriteStorageCommand<PlayHistoryRecord, ChangeResult, Boolean>{

    @Inject
    public WritePlayHistoryCommand(PropellerDatabase propeller) {
        super(propeller);
    }

    @Override
    protected ChangeResult write(PropellerDatabase propeller, PlayHistoryRecord input) {
        return propeller.upsert(Tables.PlayHistory.TABLE, buildContentValue(input));
    }

    private ContentValues buildContentValue(PlayHistoryRecord record) {
        return ContentValuesBuilder.values()
                .put(Tables.PlayHistory.TIMESTAMP, record.timestamp())
                .put(Tables.PlayHistory.TRACK_ID, record.trackUrn().getNumericId())
                .put(Tables.PlayHistory.CONTEXT_TYPE, record.getContextType())
                .put(Tables.PlayHistory.CONTEXT_ID, record.contextUrn().getNumericId())
                .get();
    }

    @Override
    protected Boolean transform(ChangeResult result) {
        return result.success();
    }

}
