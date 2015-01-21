package com.soundcloud.android.offline.commands;

import static android.provider.BaseColumns._ID;
import static com.soundcloud.android.storage.Table.TrackDownloads;
import static com.soundcloud.android.storage.TableColumns.TrackDownloads.DOWNLOADED_AT;

import com.soundcloud.android.commands.StoreCommand;
import com.soundcloud.android.offline.DownloadResult;
import com.soundcloud.propeller.ContentValuesBuilder;
import com.soundcloud.propeller.PropellerDatabase;
import com.soundcloud.propeller.WriteResult;

import android.content.ContentValues;

import javax.inject.Inject;

public class StoreCompletedDownloadCommand extends StoreCommand<DownloadResult> {

    @Inject
    StoreCompletedDownloadCommand(PropellerDatabase database) {
        super(database);
    }

    @Override
    protected WriteResult store() {
        return database.upsert(TrackDownloads, buildContentValues(input));
    }

    private ContentValues buildContentValues(DownloadResult downloadResult) {
        return ContentValuesBuilder.values(2)
                .put(_ID, downloadResult.getUrn().getNumericId())
                .put(DOWNLOADED_AT, downloadResult.getDownloadedAt())
                .get();
    }
}
