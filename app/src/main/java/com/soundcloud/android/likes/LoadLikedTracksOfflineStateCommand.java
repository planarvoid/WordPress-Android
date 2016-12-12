package com.soundcloud.android.likes;

import com.soundcloud.android.commands.Command;
import com.soundcloud.android.offline.OfflineState;
import com.soundcloud.android.offline.OfflineStateMapper;
import com.soundcloud.android.storage.Tables;
import com.soundcloud.propeller.CursorReader;
import com.soundcloud.propeller.PropellerDatabase;
import com.soundcloud.propeller.query.Query;
import com.soundcloud.propeller.rx.RxResultMapper;

import javax.inject.Inject;
import java.util.Collection;

public class LoadLikedTracksOfflineStateCommand extends Command<Void, Collection<OfflineState>> {
    private final PropellerDatabase propeller;

    @Inject
    LoadLikedTracksOfflineStateCommand(PropellerDatabase propeller) {
        this.propeller = propeller;
    }

    @Override
    public Collection<OfflineState> call(Void input) {
        return propeller.query(trackLikeQuery()).toList(new RxResultMapper<OfflineState>() {
            @Override
            public OfflineState map(CursorReader reader) {
                return OfflineStateMapper.fromDates(reader, true);
            }
        });
    }

    private static Query trackLikeQuery() {
        return Query.from(Tables.Likes.TABLE)
                    .select(Tables.TrackDownloads.REQUESTED_AT,
                            Tables.TrackDownloads.DOWNLOADED_AT,
                            Tables.TrackDownloads.UNAVAILABLE_AT,
                            Tables.TrackDownloads.REMOVED_AT)
                    .leftJoin(Tables.TrackDownloads.TABLE,
                              Tables.Likes._ID,
                              Tables.TrackDownloads._ID)
                    .whereEq(Tables.Likes._TYPE, Tables.Sounds.TYPE_TRACK)
                    .whereNull(Tables.Likes.REMOVED_AT);
    }
}
