package com.soundcloud.android.offline;

import static com.soundcloud.propeller.query.ColumnFunctions.exists;
import static com.soundcloud.propeller.rx.RxResultMapper.scalar;

import com.soundcloud.android.commands.Command;
import com.soundcloud.android.storage.Tables;
import com.soundcloud.propeller.PropellerDatabase;
import com.soundcloud.propeller.query.Query;

import javax.inject.Inject;

class IsOfflineLikedTracksEnabledCommand extends Command<Void, Boolean> {
    private static final String IS_OFFLINE_LIKES = "if_offline_likes";
    private final PropellerDatabase propeller;

    @Inject
    IsOfflineLikedTracksEnabledCommand(PropellerDatabase propeller) {
        this.propeller = propeller;
    }

    @Override
    public Boolean call(Void ignored) {
        return propeller.query(isOfflineLikesEnabledQuery()).first(scalar(Boolean.class));
    }

    static Query isOfflineLikesEnabledQuery() {
        return Query.apply(exists(Query.from(Tables.OfflineContent.TABLE)
                .where(OfflineContentStorage.offlineLikesFilter()))
                .as(IS_OFFLINE_LIKES));
    }
}

