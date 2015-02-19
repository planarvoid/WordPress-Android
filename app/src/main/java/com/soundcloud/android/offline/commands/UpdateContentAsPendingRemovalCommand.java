package com.soundcloud.android.offline.commands;

import static com.soundcloud.android.storage.Table.TrackDownloads;

import com.soundcloud.android.commands.StoreCommand;
import com.soundcloud.android.storage.TableColumns;
import com.soundcloud.propeller.ContentValuesBuilder;
import com.soundcloud.propeller.PropellerDatabase;
import com.soundcloud.propeller.WriteResult;
import com.soundcloud.propeller.query.WhereBuilder;

import android.content.ContentValues;

import javax.inject.Inject;

public class UpdateContentAsPendingRemovalCommand extends StoreCommand<Object> {

    @Inject
    protected UpdateContentAsPendingRemovalCommand(PropellerDatabase database) {
        super(database);
    }

    @Override
    protected WriteResult store() {
        return database.update(TrackDownloads, buildPendingRemoval(), new WhereBuilder());
    }

    private ContentValues buildPendingRemoval() {
        final long now = System.currentTimeMillis();
        return ContentValuesBuilder
                .values()
                .put(TableColumns.TrackDownloads.REMOVED_AT, now)
                .get();
    }
}
