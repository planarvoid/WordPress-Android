package com.soundcloud.android.users;

import com.soundcloud.android.model.Urn;
import com.soundcloud.android.storage.Tables;
import com.soundcloud.java.checks.Preconditions;
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

    @SuppressWarnings("PMD.SimplifyStartsWith")
    public Maybe<Urn> urnForPermalink(String permalink) {
        Preconditions.checkArgument(!permalink.startsWith("/"), "Permalink must not start with a '/' and must not be a url.");
        return propeller.queryResult(buildPermalinkQuery(permalink))
                        .filter(queryResult -> !queryResult.isEmpty())
                        .map(queryResult -> queryResult.first(cursorReader -> Urn.forUser(cursorReader.getLong(Tables.UsersView.ID))))
                        .firstElement();
    }

    private Query buildPermalinkQuery(String identifier) {
        return Query.from(Tables.UsersView.TABLE)
                    .select(Tables.UsersView.ID)
                    .whereIn(Tables.UsersView.PERMALINK, identifier)
                    .limit(1);
    }

    private Query buildUserQuery(Urn userUrn) {
        return Query.from(Tables.UsersView.TABLE)
                    .select("*")
                    .whereEq(Tables.UsersView.ID, userUrn.getNumericId());
    }
}
