package com.soundcloud.android.collection;

import static com.soundcloud.android.utils.PropertySets.extractIds;
import static com.soundcloud.android.utils.Urns.playlistPredicate;
import static com.soundcloud.propeller.query.ColumnFunctions.exists;

import com.soundcloud.android.commands.Command;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.storage.Table;
import com.soundcloud.android.storage.TableColumns;
import com.soundcloud.android.storage.Tables;
import com.soundcloud.java.optional.Optional;
import com.soundcloud.propeller.CursorReader;
import com.soundcloud.propeller.PropellerDatabase;
import com.soundcloud.propeller.QueryResult;
import com.soundcloud.propeller.query.Query;

import javax.inject.Inject;
import java.util.HashMap;
import java.util.Map;

public class LoadPlaylistLikedStatuses extends Command<Iterable<Urn>, Map<Urn, Boolean>> {

    private static final String COLUMN_IS_LIKED = "is_liked";

    private final PropellerDatabase propeller;

    @Inject
    public LoadPlaylistLikedStatuses(PropellerDatabase propeller) {
        this.propeller = propeller;
    }

    @Override
    public Map<Urn, Boolean> call(Iterable<Urn> input) {
        final QueryResult query = propeller.query(forLikes(input));
        return toLikedSet(query);
    }

    private Query forLikes(Iterable<Urn> input) {
        final Query isLiked = Query.from(Tables.Likes.TABLE)
                                   .joinOn(Table.SoundView.field(TableColumns.SoundView._ID),
                                           Tables.Likes._ID.qualifiedName())
                                   .whereEq(Tables.Likes._TYPE.qualifiedName(),
                                            Tables.Sounds.TYPE_PLAYLIST)
                                   .whereNull(Tables.Likes.REMOVED_AT.qualifiedName());

        return Query.from(Table.SoundView.name())
                    .select(TableColumns.SoundView._ID, exists(isLiked).as(COLUMN_IS_LIKED))
                    .whereIn(TableColumns.SoundView._ID, extractIds(input, Optional.of(playlistPredicate())))
                    .whereEq(TableColumns.SoundView._TYPE, Tables.Sounds.TYPE_PLAYLIST);
    }

    private Map<Urn, Boolean> toLikedSet(QueryResult result) {
        Map<Urn, Boolean> likedMap = new HashMap<>();
        for (CursorReader reader : result) {
            final Urn playlistUrn = Urn.forPlaylist(reader.getLong(TableColumns.SoundView._ID));
            likedMap.put(playlistUrn, reader.getBoolean(COLUMN_IS_LIKED));
        }
        return likedMap;
    }
}
