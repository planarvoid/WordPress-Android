package com.soundcloud.android.likes;

import com.soundcloud.android.model.Urn;
import com.soundcloud.android.storage.BaseRxResultMapperV2;
import com.soundcloud.android.storage.Tables;
import com.soundcloud.propeller.CursorReader;
import com.soundcloud.propeller.query.Query;
import com.soundcloud.propeller.rx.PropellerRxV2;
import io.reactivex.Single;

import javax.inject.Inject;
import java.util.List;

class LikesStorage {

    private final PropellerRxV2 propellerRx;
    private final BaseRxResultMapperV2<Urn> likesByUrnMapper = new BaseRxResultMapperV2<Urn>() {
        @Override
        public Urn map(CursorReader reader) {
            return readSoundUrn(reader, Tables.Likes._ID, Tables.Likes._TYPE);
        }
    };

    @Inject
    LikesStorage(PropellerRxV2 propellerRx) {
        this.propellerRx = propellerRx;
    }

    Single<List<Urn>> loadLikes() {
        return propellerRx.queryResult(Query.from(Tables.Likes.TABLE)
                                            .select(Tables.Likes._ID, Tables.Likes._TYPE))
                          .map(cursorReaders -> cursorReaders.toList(likesByUrnMapper))
                          .firstOrError();
    }
}
