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
import com.soundcloud.android.users.UserProperty;
import com.soundcloud.java.collections.PropertySet;
import com.soundcloud.propeller.CursorReader;
import com.soundcloud.propeller.PropellerDatabase;
import com.soundcloud.propeller.QueryResult;
import com.soundcloud.propeller.query.Query;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

class LoadFollowingCommand extends Command<Iterable<PropertySet>, Map<Urn, PropertySet>> {
    private final PropellerDatabase propeller;

    @Inject
    public LoadFollowingCommand(PropellerDatabase propeller) {
        this.propeller = propeller;
    }

    @Override
    public Map<Urn, PropertySet> call(Iterable<PropertySet> input) {
        final QueryResult query = propeller.query(forFollowings(input));
        return toFollowingSet(query);
    }

    private Query forFollowings(Iterable<PropertySet> input) {
        return Query.from(TABLE)
                    .whereEq(ASSOCIATION_TYPE,
                             TYPE_FOLLOWING)
                    .whereIn(TARGET_ID, getUserIds(input))
                    .whereNull(REMOVED_AT)
                    .order(POSITION, ASC);
    }

    private List<Long> getUserIds(Iterable<PropertySet> propertySets) {
        final List<Long> userIds = new ArrayList<>();
        for (PropertySet set : propertySets) {
            final Urn urn = set.getOrElse(UserProperty.URN, Urn.NOT_SET);
            if (urn.isUser()) {
                userIds.add(urn.getNumericId());
            }
        }
        return userIds;
    }

    private Map<Urn, PropertySet> toFollowingSet(QueryResult result) {
        Map<Urn, PropertySet> followingsMap = new HashMap<>();
        for (CursorReader reader : result) {
            final Urn userUrn = Urn.forUser(reader.getLong(TARGET_ID));
            followingsMap.put(userUrn, PropertySet.from(UserProperty.IS_FOLLOWED_BY_ME.bind(true)));
        }
        return followingsMap;
    }
}
