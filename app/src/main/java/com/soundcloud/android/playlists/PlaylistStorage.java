package com.soundcloud.android.playlists;

import static com.soundcloud.android.storage.CollectionStorage.CollectionItemTypes;
import static com.soundcloud.android.storage.TableColumns.CollectionItems;
import static com.soundcloud.android.storage.TableColumns.PlaylistTracks;
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
import com.soundcloud.propeller.rx.DatabaseScheduler;
import com.soundcloud.propeller.rx.RxResultMapper;
import rx.Observable;
import rx.Scheduler;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;

public class PlaylistStorage {
    private static final String COLUMN_IS_LIKED = "is_liked";

    private final DatabaseScheduler scheduler;
    private final PropellerDatabase database;


    @Inject
    public PlaylistStorage(PropellerDatabase database, Scheduler scheduler) {
        this.database = database;
        this.scheduler = new DatabaseScheduler(database, scheduler);
    }

    public Observable<Urn> trackUrns(final Urn playlistUrn) {
        Query query = Query.from(Table.PLAYLIST_TRACKS.name)
                .select(PlaylistTracks.TRACK_ID)
                .whereEq(PlaylistTracks.PLAYLIST_ID, playlistUrn.getNumericId())
                .order(PlaylistTracks.POSITION, Query.ORDER_ASC);
        return scheduler.scheduleQuery(query).map(new TrackUrnMapper());
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
        final Query isLiked = Query.from(Table.COLLECTION_ITEMS.name)
                .joinOn(Table.SOUND_VIEW.name + "." + SoundView._ID, CollectionItems.ITEM_ID)
                .joinOn(SoundView._TYPE, CollectionItems.RESOURCE_TYPE)
                .whereEq(CollectionItems.COLLECTION_TYPE, CollectionItemTypes.LIKE);

        return Query.from(Table.SOUND_VIEW.name)
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

    private static final class TrackUrnMapper extends RxResultMapper<Urn> {
        @Override
        public Urn map(CursorReader cursorReader) {
            return Urn.forTrack(cursorReader.getLong(PlaylistTracks.TRACK_ID));
        }
    }
}
