package com.soundcloud.android.suggestedcreators;

import com.soundcloud.android.storage.Tables;
import com.soundcloud.android.users.User;
import com.soundcloud.propeller.CursorReader;
import com.soundcloud.propeller.query.Query;
import com.soundcloud.propeller.rx.PropellerRx;
import com.soundcloud.propeller.rx.RxResultMapper;
import rx.Observable;

import javax.inject.Inject;
import java.util.List;

class SuggestedCreatorsStorage {
    private final PropellerRx propellerRx;

    @Inject
    SuggestedCreatorsStorage(PropellerRx propellerRx) {
        this.propellerRx = propellerRx;
    }

    Observable<List<SuggestedCreator>> suggestedCreators() {
        return propellerRx.query(buildSuggestedCreatorsQuery()).map(new SuggestedCreatorMapper()).toList();
    }

    private Query buildSuggestedCreatorsQuery() {
        return Query.from(Tables.SuggestedCreators.TABLE)
                    .leftJoin(Tables.UsersView.TABLE, Tables.SuggestedCreators.SEED_USER_ID, Tables.UsersView.ID)
                    .select("*");
    }

    private class SuggestedCreatorMapper extends RxResultMapper<SuggestedCreator> {

        @Override
        public SuggestedCreator map(CursorReader reader) {
            final User userRecord = User.fromCursorReader(reader);
            final String relation = reader.getString(Tables.SuggestedCreators.RELATION_KEY.name());
            return SuggestedCreator.create(userRecord, SuggestedCreatorRelation.from(relation));
        }
    }
}
