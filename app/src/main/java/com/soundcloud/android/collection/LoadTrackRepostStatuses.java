package com.soundcloud.android.collection;

import static com.soundcloud.android.utils.PropertySets.extractIds;
import static com.soundcloud.android.utils.Urns.trackPredicate;
import static com.soundcloud.propeller.query.Filter.filter;

import com.soundcloud.android.commands.Command;
import com.soundcloud.android.model.PlayableProperty;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.storage.Table;
import com.soundcloud.android.storage.TableColumns;
import com.soundcloud.java.collections.PropertySet;
import com.soundcloud.java.optional.Optional;
import com.soundcloud.propeller.CursorReader;
import com.soundcloud.propeller.PropellerDatabase;
import com.soundcloud.propeller.QueryResult;
import com.soundcloud.propeller.query.Query;
import com.soundcloud.propeller.query.Where;

import javax.inject.Inject;
import java.util.HashMap;
import java.util.Map;

public class LoadTrackRepostStatuses extends Command<Iterable<PropertySet>, Map<Urn, PropertySet>> {

    private PropellerDatabase propeller;

    @Inject
    public LoadTrackRepostStatuses(PropellerDatabase propeller) {
        this.propeller = propeller;
    }

    @Override
    public Map<Urn, PropertySet> call(Iterable<PropertySet> input) {
        return toRepostedSet(propeller.query(forReposts(input)));
    }

    private Query forReposts(Iterable<PropertySet> input) {
        return Query.from(Table.SoundView.name())
                .select(TableColumns.SoundView._ID, TableColumns.Posts.TYPE)
                .leftJoin(Table.Posts.name(), joinCondition())
                .whereIn(TableColumns.SoundView._ID, extractIds(input, Optional.of(trackPredicate())));
    }

    private static Where joinCondition() {
        return filter().whereEq(TableColumns.SoundView._ID, TableColumns.Posts.TARGET_ID)
                .whereEq(Table.Posts.field(TableColumns.Posts.TARGET_TYPE), TableColumns.Sounds.TYPE_TRACK)
                .whereNull(Table.Posts.field(TableColumns.Likes.REMOVED_AT));
    }

    private Map<Urn, PropertySet> toRepostedSet(QueryResult queryResult) {
        Map<Urn, PropertySet> result = new HashMap<>();
        for (CursorReader reader : queryResult) {
            final Urn trackUrn = Urn.forTrack(reader.getLong(TableColumns.SoundView._ID));
            result.put(trackUrn, PropertySet.from(PlayableProperty.IS_USER_REPOST.bind(isReposted(reader))));
        }
        return result;
    }

    private boolean isReposted(CursorReader reader) {
        return reader.isNotNull(TableColumns.Posts.TYPE) &&
                reader.getString(TableColumns.Posts.TYPE).equals(TableColumns.Posts.TYPE_REPOST);
    }
}
