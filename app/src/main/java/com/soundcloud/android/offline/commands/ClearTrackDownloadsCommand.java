package com.soundcloud.android.offline.commands;

import static android.provider.BaseColumns._ID;

import com.soundcloud.android.commands.Command;
import com.soundcloud.android.commands.TrackUrnMapper;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.offline.SecureFileStorage;
import com.soundcloud.android.storage.Table;
import com.soundcloud.propeller.CursorReader;
import com.soundcloud.propeller.PropellerDatabase;
import com.soundcloud.propeller.TxnResult;
import com.soundcloud.propeller.query.Query;
import com.soundcloud.propeller.rx.RxResultMapper;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ClearTrackDownloadsCommand extends Command<Void, List<Urn>> {

    private final PropellerDatabase propeller;
    private final SecureFileStorage secureFileStorage;

    @Inject
    ClearTrackDownloadsCommand(PropellerDatabase propeller, SecureFileStorage secureFileStorage) {
        this.propeller = propeller;
        this.secureFileStorage = secureFileStorage;
    }

    @Override
    public List<Urn> call(Void input) {
        List<Urn> removedEntities = queryEntitiesToRemove(propeller);

        TxnResult txnResult = propeller.runTransaction(new PropellerDatabase.Transaction() {
            @Override
            public void steps(PropellerDatabase propeller) {

                step(propeller.delete(Table.TrackDownloads));
                step(propeller.delete(Table.OfflineContent));

                secureFileStorage.deleteAllTracks();
            }
        });

        return txnResult.success() ? removedEntities : Collections.<Urn>emptyList();
    }

    private List<Urn> queryEntitiesToRemove(PropellerDatabase propeller) {
        final List<Urn> result = new ArrayList<>();
        result.addAll(queryOfflinePlaylistsUrns(propeller));
        result.addAll(queryOfflineTracksUrns(propeller));

        return result;
    }

    private List<Urn> queryOfflineTracksUrns(PropellerDatabase propeller) {
        return propeller.query(Query.from(Table.TrackDownloads.name())).toList(new TrackUrnMapper());
    }

    private List<Urn> queryOfflinePlaylistsUrns(PropellerDatabase propeller) {
        return propeller.query(Query.from(Table.OfflineContent.name())).toList(new PlaylistUrnMapper());
    }

    private final class PlaylistUrnMapper extends RxResultMapper<Urn> {
        @Override
        public Urn map(CursorReader cursorReader) {
            return Urn.forPlaylist(cursorReader.getLong(_ID));
        }
    }
}
