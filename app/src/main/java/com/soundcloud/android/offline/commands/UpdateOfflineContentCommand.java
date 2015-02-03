package com.soundcloud.android.offline.commands;

import static android.provider.BaseColumns._ID;
import static com.soundcloud.android.storage.Table.TrackDownloads;
import static com.soundcloud.android.storage.TableColumns.TrackDownloads.REMOVED_AT;

import com.soundcloud.android.commands.StoreCommand;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.storage.TableColumns;
import com.soundcloud.propeller.ContentValuesBuilder;
import com.soundcloud.propeller.PropellerDatabase;
import com.soundcloud.propeller.WriteResult;
import com.soundcloud.propeller.query.WhereBuilder;

import android.content.ContentValues;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class UpdateOfflineContentCommand extends StoreCommand<List<Urn>> {

    @Inject
    protected UpdateOfflineContentCommand(PropellerDatabase database) {
        super(database);
    }

    @Override
    protected WriteResult store() {
        return database.runTransaction(new PropellerDatabase.Transaction() {
            @Override
            public void steps(PropellerDatabase database) {
                markAllEntriesAsPendingRemoval(database);
                markAllInputAsDownloads(database);
            }

            private void markAllInputAsDownloads(PropellerDatabase database) {
                if (input != null){
                    for (ContentValues cv : buildContentValuesFromUrns(input)) {
                        step(database.upsert(TrackDownloads, cv));
                    }
                }
            }

            private void markAllEntriesAsPendingRemoval(PropellerDatabase database) {
                step(database.update(TrackDownloads, buildPendingRemoval(), new WhereBuilder().whereNull(REMOVED_AT)));
            }
        });
    }

    private ContentValues buildPendingRemoval() {
        final long now = System.currentTimeMillis();
        return ContentValuesBuilder
                .values()
                .put(TableColumns.TrackDownloads.REMOVED_AT, now)
                .get();
    }

    private List<ContentValues> buildContentValuesFromUrns(final Collection<Urn> downloadRequests) {
        final List<ContentValues> newItems = new ArrayList<>(downloadRequests.size());

        for (Urn urn : downloadRequests) {
            newItems.add(ContentValuesBuilder.values()
                    .put(_ID, urn.getNumericId())
                    .put(REMOVED_AT, null)
                    .get());
        }
        return newItems;
    }
}
