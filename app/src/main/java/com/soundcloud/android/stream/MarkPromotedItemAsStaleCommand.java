package com.soundcloud.android.stream;

import static com.soundcloud.propeller.query.Filter.filter;

import com.soundcloud.android.commands.DefaultWriteStorageCommand;
import com.soundcloud.android.model.PromotedItemProperty;
import com.soundcloud.android.presentation.PromotedListItem;
import com.soundcloud.android.storage.Table;
import com.soundcloud.android.storage.TableColumns;
import com.soundcloud.java.collections.PropertySet;
import com.soundcloud.propeller.PropellerDatabase;
import com.soundcloud.propeller.WriteResult;
import com.soundcloud.propeller.query.Where;

import android.content.ContentValues;

import javax.inject.Inject;

/**
 * @see RemoveStalePromotedItemsCommand
 * @see SoundStreamOperations#promotedImpressionAction
 */
public class MarkPromotedItemAsStaleCommand extends DefaultWriteStorageCommand<PromotedListItem, WriteResult> {

    @Inject
    protected MarkPromotedItemAsStaleCommand(PropellerDatabase propeller) {
        super(propeller);
    }

    @Override
    protected WriteResult write(PropellerDatabase database, PromotedListItem item) {
        ContentValues values = new ContentValues();
        values.put(TableColumns.PromotedTracks.CREATED_AT, 0L);
        Where where = filter().whereEq(TableColumns.PromotedTracks.AD_URN, item.getAdUrn());
        return database.update(Table.PromotedTracks, values, where);
    }
}
