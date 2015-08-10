package com.soundcloud.android.offline;

import static com.soundcloud.android.offline.OfflineContentStorage.offlineLikesFilter;
import static com.soundcloud.android.storage.TableColumns.OfflineContent._TYPE;

import com.soundcloud.android.commands.Command;
import com.soundcloud.android.commands.PlaylistUrnMapper;
import com.soundcloud.android.commands.TrackUrnMapper;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.storage.Table;
import com.soundcloud.android.storage.TableColumns;
import com.soundcloud.android.storage.Tables;
import com.soundcloud.propeller.PropellerDatabase;
import com.soundcloud.propeller.TxnResult;
import com.soundcloud.propeller.query.Filter;
import com.soundcloud.propeller.query.Query;
import com.soundcloud.propeller.query.Where;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

class ClearTrackDownloadsCommand extends Command<Void, List<Urn>> {

    private final PropellerDatabase propeller;
    private final SecureFileStorage secureFileStorage;

    @Inject
    ClearTrackDownloadsCommand(PropellerDatabase propeller, SecureFileStorage secureFileStorage) {
        this.propeller = propeller;
        this.secureFileStorage = secureFileStorage;
    }

    @Override
    public List<Urn> call(Void input) {
        final List<Urn> removedEntities = queryEntitiesToRemove(propeller);

        final TxnResult txnResult = propeller.runTransaction(new PropellerDatabase.Transaction() {
            @Override
            public void steps(PropellerDatabase propeller) {
                step(propeller.delete(Table.OfflineContent, offlineLikesFilter()));
                step(propeller.delete(Tables.TrackDownloads.TABLE));
                step(propeller.delete(Table.OfflineContent));
            }
        });

        if (txnResult.success()) {
            secureFileStorage.deleteAllTracks();
            return removedEntities;
        }

        return Collections.emptyList();
    }

    private List<Urn> queryEntitiesToRemove(PropellerDatabase propeller) {
        final List<Urn> result = new ArrayList<>();
        result.addAll(queryOfflinePlaylistsUrns(propeller));
        result.addAll(queryOfflineTracksUrns(propeller));

        return result;
    }

    private List<Urn> queryOfflineTracksUrns(PropellerDatabase propeller) {
        return propeller.query(Query.from(Tables.TrackDownloads.TABLE)).toList(new TrackUrnMapper());
    }

    private List<Urn> queryOfflinePlaylistsUrns(PropellerDatabase propeller) {
        final Where isOfflinePlaylist = Filter.filter()
                .whereEq(_TYPE, TableColumns.OfflineContent.TYPE_PLAYLIST);

        return propeller.query(Query.from(Table.OfflineContent.name())
                .where(isOfflinePlaylist))
                .toList(new PlaylistUrnMapper());
    }

}
