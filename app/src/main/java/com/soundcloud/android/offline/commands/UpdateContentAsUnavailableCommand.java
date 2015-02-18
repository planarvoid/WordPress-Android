package com.soundcloud.android.offline.commands;

import static com.soundcloud.android.storage.Table.TrackDownloads;

import com.soundcloud.android.commands.StoreCommand;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.storage.TableColumns;
import com.soundcloud.propeller.ContentValuesBuilder;
import com.soundcloud.propeller.PropellerDatabase;
import com.soundcloud.propeller.WriteResult;
import com.soundcloud.propeller.query.WhereBuilder;

import android.content.ContentValues;

import javax.inject.Inject;

public class UpdateContentAsUnavailableCommand extends StoreCommand<Urn> {

    @Inject
    protected UpdateContentAsUnavailableCommand(PropellerDatabase database) {
        super(database);
    }

    @Override
    protected WriteResult store() {
        return database.update(TrackDownloads, buildUnavailable(), new WhereBuilder().whereEq(TableColumns.TrackDownloads._ID, input.getNumericId()));
    }

    private ContentValues buildUnavailable() {
        final long now = System.currentTimeMillis();
        return ContentValuesBuilder
                .values()
                .put(TableColumns.TrackDownloads.UNAVAILABLE_AT, now)
                .get();
    }
}
