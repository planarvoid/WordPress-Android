package com.soundcloud.android.users;

import com.soundcloud.android.model.Urn;
import com.soundcloud.android.storage.Tables;
import com.soundcloud.propeller.CursorReader;
import com.soundcloud.propeller.query.Query;
import com.soundcloud.propeller.rx.PropellerRx;
import com.soundcloud.propeller.rx.RxResultMapper;
import rx.Observable;

import javax.inject.Inject;

public class UserStorage {

    private final PropellerRx propeller;

    @Inject
    UserStorage(PropellerRx propeller) {
        this.propeller = propeller;
    }

    Observable<User> loadUser(Urn urn) {
        return propeller.query(buildUserQuery(urn))
                        .map(new RxResultMapper<User>() {
                            @Override
                            public User map(CursorReader cursorReader) {
                                return User.fromCursorReader(cursorReader);
                            }
                        });
    }

    private Query buildUserQuery(Urn userUrn) {
        return Query.from(Tables.UsersView.TABLE)
                    .select("*")
                    .whereEq(Tables.UsersView.ID, userUrn.getNumericId());
    }
}
