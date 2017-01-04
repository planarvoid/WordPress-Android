package com.soundcloud.android.likes;

import com.soundcloud.android.model.Urn;
import com.soundcloud.android.storage.BaseRxResultMapper;
import com.soundcloud.android.storage.Tables;
import com.soundcloud.propeller.CursorReader;
import com.soundcloud.propeller.query.Query;
import com.soundcloud.propeller.rx.PropellerRx;
import rx.Observable;

import java.util.List;

class LikesStorage {

    private final PropellerRx propellerRx;
    private final BaseRxResultMapper<Urn> likesByUrnMapper = new BaseRxResultMapper<Urn>() {
        @Override
        public Urn map(CursorReader reader) {
            return readSoundUrn(reader, Tables.Likes._ID, Tables.Likes._TYPE);
        }
    };

    LikesStorage(PropellerRx propellerRx) {
        this.propellerRx = propellerRx;
    }

    Observable<List<Urn>> loadLikes() {
        return propellerRx.query(Query.from(Tables.Likes.TABLE)
                                      .select(Tables.Likes._ID, Tables.Likes._TYPE))
                          .map(likesByUrnMapper).toList();
    }
}
