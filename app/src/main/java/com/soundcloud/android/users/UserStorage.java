package com.soundcloud.android.users;

import com.soundcloud.android.model.Urn;
import com.soundcloud.android.storage.Tables;
import com.soundcloud.propeller.query.Query;
import com.soundcloud.propeller.rx.PropellerRxV2;
import io.reactivex.Maybe;
import io.reactivex.Observable;

import javax.inject.Inject;

public class UserStorage {

    private final PropellerRxV2 propeller;

    @Inject
    UserStorage(PropellerRxV2 propeller) {
        this.propeller = propeller;
    }

    Maybe<User> loadUser(Urn urn) {
        return propeller.queryResult(buildUserQuery(urn))
                        .flatMap(cursorReaders -> Observable.fromIterable(cursorReaders.toList(User::fromCursorReader)))
                        .firstElement();
    }

    private Query buildUserQuery(Urn userUrn) {
        return Query.from(Tables.UsersView.TABLE)
                    .select("*")
                    .whereEq(Tables.UsersView.ID, userUrn.getNumericId());
    }
}
