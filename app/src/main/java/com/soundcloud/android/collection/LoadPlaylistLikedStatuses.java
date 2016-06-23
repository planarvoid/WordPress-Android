package com.soundcloud.android.collection;

import static com.soundcloud.android.utils.PropertySets.extractIds;
import static com.soundcloud.android.utils.Urns.playlistPredicate;
import static com.soundcloud.propeller.query.ColumnFunctions.exists;

import com.soundcloud.android.commands.Command;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playlists.PlaylistProperty;
import com.soundcloud.android.storage.Table;
import com.soundcloud.android.storage.TableColumns;
import com.soundcloud.android.utils.PropertySets;
import com.soundcloud.android.utils.Urns;
import com.soundcloud.java.collections.PropertySet;
import com.soundcloud.java.functions.Predicate;
import com.soundcloud.java.optional.Optional;
import com.soundcloud.propeller.CursorReader;
import com.soundcloud.propeller.PropellerDatabase;
import com.soundcloud.propeller.QueryResult;
import com.soundcloud.propeller.query.Query;

import javax.inject.Inject;
import java.util.HashMap;
import java.util.Map;

public class LoadPlaylistLikedStatuses extends Command<Iterable<PropertySet>, Map<Urn, PropertySet>> {

    private static final String COLUMN_IS_LIKED = "is_liked";

    private final PropellerDatabase propeller;

    @Inject
    public LoadPlaylistLikedStatuses(PropellerDatabase propeller) {
        this.propeller = propeller;
    }

    @Override
    public Map<Urn, PropertySet> call(Iterable<PropertySet> input) {
        final QueryResult query = propeller.query(forLikes(input));
        return toLikedSet(query);
    }

    private Query forLikes(Iterable<PropertySet> input) {
        final Query isLiked = Query.from(Table.Likes.name())
                                   .joinOn(Table.SoundView + "." + TableColumns.SoundView._ID,
                                           Table.Likes.name() + "." + TableColumns.Likes._ID)
                                   .whereEq(Table.Likes + "." + TableColumns.Likes._TYPE,
                                            TableColumns.Sounds.TYPE_PLAYLIST)
                                   .whereNull(Table.Likes.field(TableColumns.Likes.REMOVED_AT));

        return Query.from(Table.SoundView.name())
                    .select(TableColumns.SoundView._ID, exists(isLiked).as(COLUMN_IS_LIKED))
                    .whereIn(TableColumns.SoundView._ID, extractIds(input, Optional.of(playlistPredicate())))
                    .whereEq(TableColumns.SoundView._TYPE, TableColumns.Sounds.TYPE_PLAYLIST);
    }

    private Map<Urn, PropertySet> toLikedSet(QueryResult result) {
        Map<Urn, PropertySet> likedMap = new HashMap<>();
        for (CursorReader reader : result) {
            final Urn playlistUrn = Urn.forPlaylist(reader.getLong(TableColumns.SoundView._ID));
            likedMap.put(playlistUrn,
                         PropertySet.from(PlaylistProperty.IS_USER_LIKE.bind(reader.getBoolean(COLUMN_IS_LIKED))));
        }
        return likedMap;
    }
}
