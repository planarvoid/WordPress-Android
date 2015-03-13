package com.soundcloud.android.offline.commands;

import static android.provider.BaseColumns._ID;
import static com.soundcloud.android.storage.Table.TrackDownloads;

import com.soundcloud.android.commands.StoreCommand;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.storage.TableColumns;
import com.soundcloud.propeller.ContentValuesBuilder;
import com.soundcloud.propeller.PropellerDatabase;
import com.soundcloud.propeller.WriteResult;

import android.content.ContentValues;

import javax.inject.Inject;
import java.util.Collection;

public class StoreDownloadedCommand extends StoreCommand<Collection<Urn>> {

    @Inject
    StoreDownloadedCommand(PropellerDatabase database) {
        super(database);
    }

    @Override
    protected WriteResult store() {
        return database.runTransaction(new PropellerDatabase.Transaction() {
            @Override
            public void steps(PropellerDatabase propeller) {
                for (Urn urn : input) {
                    step(database.upsert(TrackDownloads, buildContentValues(urn)));
                }
            }
        });
    }

    private ContentValues buildContentValues(Urn urn) {
        return ContentValuesBuilder.values(2)
                .put(_ID, urn.getNumericId())
                .put(TableColumns.TrackDownloads.REMOVED_AT, null)
                .put(TableColumns.TrackDownloads.DOWNLOADED_AT, System.currentTimeMillis())
                .get();
    }
}
