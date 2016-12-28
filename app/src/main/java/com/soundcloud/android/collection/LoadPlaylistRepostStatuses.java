package com.soundcloud.android.collection;

import static com.soundcloud.android.storage.TableColumns.SoundView;
import static com.soundcloud.android.utils.PropertySets.extractIds;
import static com.soundcloud.android.utils.Urns.playlistPredicate;
import static com.soundcloud.propeller.query.Filter.filter;

import com.soundcloud.android.commands.Command;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.storage.Table;
import com.soundcloud.android.storage.Tables;
import com.soundcloud.android.storage.Tables.Posts;
import com.soundcloud.java.optional.Optional;
import com.soundcloud.propeller.CursorReader;
import com.soundcloud.propeller.PropellerDatabase;
import com.soundcloud.propeller.QueryResult;
import com.soundcloud.propeller.query.Query;
import com.soundcloud.propeller.query.Where;

import javax.inject.Inject;
import java.util.HashMap;
import java.util.Map;

public class LoadPlaylistRepostStatuses extends Command<Iterable<Urn>, Map<Urn, Boolean>> {

    private final PropellerDatabase propeller;

    @Inject
    public LoadPlaylistRepostStatuses(PropellerDatabase propeller) {
        this.propeller = propeller;
    }

    @Override
    public Map<Urn, Boolean> call(Iterable<Urn> input) {
        return toRepostedSet(propeller.query(forReposts(input)));
    }

    private Query forReposts(Iterable<Urn> input) {
        return Query.from(Table.SoundView.name())
                    .select(SoundView._ID, Posts.TYPE)
                    .leftJoin(Posts.TABLE, joinCondition())
                    .whereIn(SoundView._ID, extractIds(input, Optional.of(playlistPredicate())));
    }

    private static Where joinCondition() {
        return filter().whereEq(SoundView._ID, Posts.TARGET_ID)
                       .whereEq(Posts.TARGET_TYPE, Tables.Sounds.TYPE_PLAYLIST)
                       .whereNull(Posts.REMOVED_AT);
    }

    private Map<Urn, Boolean> toRepostedSet(QueryResult queryResult) {
        Map<Urn, Boolean> result = new HashMap<>();
        for (CursorReader reader : queryResult) {
            final Urn playlistUrn = Urn.forPlaylist(reader.getLong(SoundView._ID));
            result.put(playlistUrn, isReposted(reader));
        }
        return result;
    }

    private boolean isReposted(CursorReader reader) {
        return reader.isNotNull(Posts.TYPE) && reader.getString(Posts.TYPE).equals(Posts.TYPE_REPOST);
    }
}
