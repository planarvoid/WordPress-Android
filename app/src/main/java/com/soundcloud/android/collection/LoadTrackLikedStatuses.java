package com.soundcloud.android.collection;

import static com.soundcloud.android.utils.PropertySets.extractIds;
import static com.soundcloud.android.utils.Urns.trackPredicate;
import static com.soundcloud.propeller.query.ColumnFunctions.exists;

import com.soundcloud.android.commands.Command;
import com.soundcloud.android.model.PlayableProperty;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.storage.Table;
import com.soundcloud.android.storage.TableColumns;
import com.soundcloud.android.storage.Tables;
import com.soundcloud.java.collections.PropertySet;
import com.soundcloud.java.optional.Optional;
import com.soundcloud.propeller.CursorReader;
import com.soundcloud.propeller.PropellerDatabase;
import com.soundcloud.propeller.QueryResult;
import com.soundcloud.propeller.query.Query;

import javax.inject.Inject;
import java.util.HashMap;
import java.util.Map;

public class LoadTrackLikedStatuses extends Command<Iterable<PropertySet>, Map<Urn, PropertySet>> {

    private static final String COLUMN_IS_LIKED = "is_liked";

    private PropellerDatabase propeller;

    @Inject
    public LoadTrackLikedStatuses(PropellerDatabase propeller) {
        this.propeller = propeller;
    }

    @Override
    public Map<Urn, PropertySet> call(Iterable<PropertySet> input) {
        final QueryResult query = propeller.query(forLikes(input));
        return toLikedSet(query);
    }

    private Query forLikes(Iterable<PropertySet> input) {
        final Query isLiked = Query.from(Tables.Likes.TABLE)
                                   .joinOn(Table.SoundView.field(TableColumns.SoundView._ID),
                                           Tables.Likes._ID.qualifiedName())
                                   .whereEq(Tables.Likes._TYPE,
                                            Tables.Sounds.TYPE_TRACK)
                                   .whereNull(Tables.Likes.REMOVED_AT);

        return Query.from(Table.SoundView.name())
                    .select(TableColumns.SoundView._ID, exists(isLiked).as(COLUMN_IS_LIKED))
                    .whereIn(TableColumns.SoundView._ID, extractIds(input, Optional.of(trackPredicate())))
                    .whereEq(TableColumns.SoundView._TYPE, Tables.Sounds.TYPE_TRACK);
    }

    private Map<Urn, PropertySet> toLikedSet(QueryResult result) {
        Map<Urn, PropertySet> likedMap = new HashMap<>();
        for (CursorReader reader : result) {
            final Urn trackUrn = Urn.forTrack(reader.getLong(TableColumns.SoundView._ID));
            likedMap.put(trackUrn,
                         PropertySet.from(PlayableProperty.IS_USER_LIKE.bind(reader.getBoolean(COLUMN_IS_LIKED))));
        }
        return likedMap;
    }
}
