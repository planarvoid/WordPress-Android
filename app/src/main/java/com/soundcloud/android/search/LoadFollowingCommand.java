package com.soundcloud.android.search;

import static com.soundcloud.android.storage.Tables.UserAssociations.ASSOCIATION_TYPE;
import static com.soundcloud.android.storage.Tables.UserAssociations.POSITION;
import static com.soundcloud.android.storage.Tables.UserAssociations.REMOVED_AT;
import static com.soundcloud.android.storage.Tables.UserAssociations.TABLE;
import static com.soundcloud.android.storage.Tables.UserAssociations.TARGET_ID;
import static com.soundcloud.android.storage.Tables.UserAssociations.TYPE_FOLLOWING;
import static com.soundcloud.propeller.query.Query.Order.ASC;

import com.soundcloud.android.commands.Command;
import com.soundcloud.android.model.Urn;
import com.soundcloud.java.collections.Iterables;
import com.soundcloud.java.collections.Lists;
import com.soundcloud.propeller.CursorReader;
import com.soundcloud.propeller.PropellerDatabase;
import com.soundcloud.propeller.QueryResult;
import com.soundcloud.propeller.query.Query;

import javax.inject.Inject;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

class LoadFollowingCommand extends Command<Iterable<Urn>, Map<Urn, Boolean>> {
    private final PropellerDatabase propeller;

    @Inject
    public LoadFollowingCommand(PropellerDatabase propeller) {
        this.propeller = propeller;
    }

    @Override
    public Map<Urn, Boolean> call(Iterable<Urn> input) {
        final QueryResult query = propeller.query(forFollowings(input));
        return toFollowingSet(query);
    }

    private Query forFollowings(Iterable<Urn> input) {
        return Query.from(TABLE)
                    .whereEq(ASSOCIATION_TYPE,
                             TYPE_FOLLOWING)
                    .whereIn(TARGET_ID, getUserIds(input))
                    .whereNull(REMOVED_AT)
                    .order(POSITION, ASC);
    }

    private Collection<Long> getUserIds(Iterable<Urn> urns) {
        return Lists.newArrayList(Iterables.transform(Iterables.filter(urns, Urn::isUser), Urn::getNumericId));
    }

    private Map<Urn, Boolean> toFollowingSet(QueryResult result) {
        Map<Urn, Boolean> followingsMap = new HashMap<>();
        for (CursorReader reader : result) {
            final Urn userUrn = Urn.forUser(reader.getLong(TARGET_ID));
            followingsMap.put(userUrn, true);
        }
        return followingsMap;
    }
}
