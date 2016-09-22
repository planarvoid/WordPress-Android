package com.soundcloud.android.users;

import com.soundcloud.android.model.Urn;
import com.soundcloud.android.storage.Tables;
import com.soundcloud.java.collections.PropertySet;
import com.soundcloud.propeller.query.Query;
import com.soundcloud.propeller.rx.PropellerRx;
import rx.Observable;

import javax.inject.Inject;

public class UserStorage {

    private final PropellerRx propeller;

    @Inject
    UserStorage(PropellerRx propeller) {
        this.propeller = propeller;
    }

    Observable<PropertySet> loadUser(Urn urn) {
        return propeller.query(buildUserQuery(urn))
                        .map(new UserMapper())
                        .firstOrDefault(PropertySet.create());
    }

    private Query buildUserQuery(Urn userUrn) {
        return Query.from(Tables.UsersView.TABLE)
                    .select("*")
                    .whereEq(Tables.UsersView.ID, userUrn.getNumericId());
    }
}
