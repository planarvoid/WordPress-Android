package com.soundcloud.android.stream;

import com.soundcloud.android.commands.DefaultWriteStorageCommand;
import com.soundcloud.android.storage.Table;
import com.soundcloud.android.storage.TableColumns;
import com.soundcloud.android.tracks.PromotedTrackProperty;
import com.soundcloud.propeller.PropellerDatabase;
import com.soundcloud.propeller.PropertySet;
import com.soundcloud.propeller.WriteResult;
import com.soundcloud.propeller.query.Filter;
import com.soundcloud.propeller.query.Where;

import android.content.ContentValues;

import javax.inject.Inject;

/**
 * @see RemoveStalePromotedTracksCommand
 * @see SoundStreamOperations#promotedImpressionAction
 */
public class MarkPromotedTrackAsStaleCommand extends DefaultWriteStorageCommand<PropertySet, WriteResult> {

    @Inject
    protected MarkPromotedTrackAsStaleCommand(PropellerDatabase propeller) {
        super(propeller);
    }

    @Override
    protected WriteResult write(PropellerDatabase database, PropertySet track) {
        ContentValues values = new ContentValues();
        values.put(TableColumns.PromotedTracks.CREATED_AT, 0L);
        Where where = Filter.filter().whereEq(TableColumns.PromotedTracks.AD_URN, track.get(PromotedTrackProperty.AD_URN));
        return database.update(Table.PromotedTracks, values, where);
    }
}
