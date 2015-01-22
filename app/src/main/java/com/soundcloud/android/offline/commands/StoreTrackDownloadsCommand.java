package com.soundcloud.android.offline.commands;

import static android.provider.BaseColumns._ID;
import static com.soundcloud.android.storage.Table.TrackDownloads;
import static com.soundcloud.android.storage.TableColumns.TrackDownloads.REQUESTED_AT;

import com.google.common.base.Predicate;
import com.google.common.collect.Collections2;
import com.soundcloud.android.commands.StoreCommand;
import com.soundcloud.android.commands.UrnMapper;
import com.soundcloud.android.model.Urn;
import com.soundcloud.propeller.ContentValuesBuilder;
import com.soundcloud.propeller.PropellerDatabase;
import com.soundcloud.propeller.WriteResult;
import com.soundcloud.propeller.query.Query;

import android.content.ContentValues;

import javax.annotation.Nullable;
import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class StoreTrackDownloadsCommand extends StoreCommand<List<Urn>> {

    @Inject
    protected StoreTrackDownloadsCommand(PropellerDatabase database) {
        super(database);
    }

    @Override
    protected WriteResult store() {
        // It looks like the SQL insert strategy with CONFLICT_IGNORE does not work as expected
        // returns -1 when the row with given ID already exists. So we do filtering out ourselves :(
        final Collection<Urn> removedDuplicates = filterDuplicates(input, getPendingDownloadUrns());
        return database.bulkInsert(TrackDownloads, buildContentValuesFromUrns(removedDuplicates));
    }

    private Collection<Urn> filterDuplicates(List<Urn> tracks, final List<Urn> existingDownloads) {
        return Collections2.filter(tracks, new Predicate<Urn>() {
            @Override
            public boolean apply(@Nullable Urn urn) {
                return !existingDownloads.contains(urn);
            }
        });
    }

    private List<Urn> getPendingDownloadUrns() {
        final Query query = Query.from(TrackDownloads.name()).select(_ID);
        return database.query(query).toList(new UrnMapper());
    }

    private List<ContentValues> buildContentValuesFromUrns(final Collection<Urn> downloadRequests) {
        final long requestedTime = System.currentTimeMillis();
        final List<ContentValues> newItems = new ArrayList<>(downloadRequests.size());

        for (Urn urn : downloadRequests) {
            newItems.add(ContentValuesBuilder.values()
                    .put(_ID, urn.getNumericId())
                    .put(REQUESTED_AT, requestedTime)
                    .get());
        }
        return newItems;
    }
}
