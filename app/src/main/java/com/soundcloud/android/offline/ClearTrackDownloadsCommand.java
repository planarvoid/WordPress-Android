package com.soundcloud.android.offline;

import com.soundcloud.android.commands.Command;
import com.soundcloud.android.commands.PlaylistUrnMapper;
import com.soundcloud.android.commands.TrackUrnMapper;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.storage.Tables;
import com.soundcloud.android.storage.Tables.OfflineContent;
import com.soundcloud.propeller.PropellerDatabase;
import com.soundcloud.propeller.TxnResult;
import com.soundcloud.propeller.query.Filter;
import com.soundcloud.propeller.query.Query;
import com.soundcloud.propeller.query.Where;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ClearTrackDownloadsCommand extends Command<Void, List<Urn>> {

    private final PropellerDatabase propeller;
    private final SecureFileStorage secureFileStorage;
    private final OfflineSettingsStorage offlineSettingsStorage;

    @Inject
    ClearTrackDownloadsCommand(PropellerDatabase propeller, SecureFileStorage secureFileStorage,
                               OfflineSettingsStorage offlineSettingsStorage) {
        this.propeller = propeller;
        this.secureFileStorage = secureFileStorage;
        this.offlineSettingsStorage = offlineSettingsStorage;
    }

    @Override
    public List<Urn> call(Void input) {
        final List<Urn> removedEntities = queryEntitiesToRemove(propeller);

        final TxnResult txnResult = propeller.runTransaction(new PropellerDatabase.Transaction() {
            @Override
            public void steps(PropellerDatabase propeller) {
                step(propeller.delete(Tables.TrackDownloads.TABLE));
                step(propeller.delete(OfflineContent.TABLE));
            }
        });

        if (txnResult.success()) {
            secureFileStorage.deleteAllTracks();
            offlineSettingsStorage.setHasOfflineContent(false);
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
                .whereEq(OfflineContent._TYPE, OfflineContent.TYPE_PLAYLIST);

        return propeller.query(Query.from(OfflineContent.TABLE)
                .where(isOfflinePlaylist))
                .toList(new PlaylistUrnMapper());
    }

}
