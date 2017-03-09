package com.soundcloud.android.stream;

import static com.soundcloud.propeller.query.Filter.filter;

import com.soundcloud.android.commands.DefaultWriteStorageCommand;
import com.soundcloud.android.storage.Table;
import com.soundcloud.android.storage.TableColumns;
import com.soundcloud.propeller.PropellerDatabase;
import com.soundcloud.propeller.WriteResult;
import com.soundcloud.propeller.query.Where;

import android.content.ContentValues;

import javax.inject.Inject;

/**
 * @see RemoveStalePromotedItemsCommand
 * @see StreamOperations#promotedImpressionAction
 */
public class MarkPromotedItemAsStaleCommand extends DefaultWriteStorageCommand<String, WriteResult> {

    @Inject
    protected MarkPromotedItemAsStaleCommand(PropellerDatabase propeller) {
        super(propeller);
    }

    @Override
    protected WriteResult write(PropellerDatabase database, String adUrn) {
        ContentValues values = new ContentValues();
        values.put(TableColumns.PromotedTracks.CREATED_AT, 0L);
        Where where = filter().whereEq(TableColumns.PromotedTracks.AD_URN, adUrn);
        return database.update(Table.PromotedTracks, values, where);
    }
}
