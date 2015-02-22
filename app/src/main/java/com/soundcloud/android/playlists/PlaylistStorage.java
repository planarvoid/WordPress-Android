package com.soundcloud.android.playlists;

import static com.soundcloud.android.storage.TableColumns.SoundView;
import static com.soundcloud.propeller.query.ColumnFunctions.exists;

import com.soundcloud.android.model.PlayableProperty;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.storage.Table;
import com.soundcloud.android.storage.TableColumns;
import com.soundcloud.propeller.CursorReader;
import com.soundcloud.propeller.PropellerDatabase;
import com.soundcloud.propeller.PropertySet;
import com.soundcloud.propeller.query.Query;
import com.soundcloud.propeller.rx.RxResultMapper;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;

@Deprecated
public class PlaylistStorage {
    private static final String COLUMN_IS_LIKED = "is_liked";

    private final PropellerDatabase database;


    @Inject
    public PlaylistStorage(PropellerDatabase database) {
        this.database = database;
    }

    /**
     * Obtains a non-order-preserving list of property sets of {URN, IS_LIKED} for the given list of input property
     * sets.
     * <p/>
     * For instance, passing the following input:
     * <pre>
     *     [{"soundcloud:tracks:1", ...}, {"soundcloud:playlists:1", ...}, {"soundcloud:users:2", ...}]
     * </pre>
     * will return a list like:
     * <pre>
     *     [{"soundcloud:playlists:1", true}]
     * </pre>
     * where 'true' in this case means "is liked by current user"
     *
     * @param input a collection of property sets representing business entities (not necessarily just playlists).
     *              They are expected to contain at least a URN property but it's safe to pass anything really.
     * @return a list of the same size or smaller than the input list containing "is liked" information for those
     * entries in the input list that represent playlists. Other items are ignored/filtered.
     */
    public List<PropertySet> playlistLikes(List<PropertySet> input) {
        final Query query = forLikes(input);
        return database.query(query).toList(new PlaylistLikeMapper());
    }

    private Query forLikes(List<PropertySet> input) {
        final Query isLiked = Query.from(Table.Likes.name())
                .joinOn(Table.SoundView + "." + TableColumns.SoundView._ID, Table.Likes.name() + "." + TableColumns.Likes._ID)
                .whereEq(Table.Likes + "." + TableColumns.Likes._TYPE, TableColumns.Sounds.TYPE_PLAYLIST)
                .whereNull(TableColumns.Likes.REMOVED_AT);

        return Query.from(Table.SoundView.name())
                .select(SoundView._ID, exists(isLiked).as(COLUMN_IS_LIKED))
                .whereIn(SoundView._ID, getPlaylistIds(input))
                .whereEq(SoundView._TYPE, TableColumns.Sounds.TYPE_PLAYLIST);
    }

    private List<Long> getPlaylistIds(List<PropertySet> propertySets) {
        final List<Long> playlistIds = new ArrayList<>(propertySets.size());
        for (PropertySet set : propertySets) {
            final Urn urn = set.getOrElse(PlayableProperty.URN, Urn.NOT_SET);
            if (urn.isPlaylist()) {
                playlistIds.add(urn.getNumericId());
            }
        }
        return playlistIds;
    }

    private static final class PlaylistLikeMapper extends RxResultMapper<PropertySet> {
        @Override
        public PropertySet map(CursorReader reader) {
            return PropertySet.from(
                    PlayableProperty.URN.bind(Urn.forPlaylist(reader.getLong(SoundView._ID))),
                    PlayableProperty.IS_LIKED.bind(reader.getBoolean(COLUMN_IS_LIKED)));
        }
    }

}
