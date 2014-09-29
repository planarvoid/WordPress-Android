package com.soundcloud.android.playlists;

import static com.soundcloud.propeller.query.ColumnFunctions.exists;

import com.google.common.base.Function;
import com.google.common.collect.Lists;
import com.soundcloud.android.model.PlayableProperty;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.storage.CollectionStorage;
import com.soundcloud.android.storage.Table;
import com.soundcloud.android.storage.TableColumns;
import com.soundcloud.propeller.CursorReader;
import com.soundcloud.propeller.PropellerDatabase;
import com.soundcloud.propeller.PropertySet;
import com.soundcloud.propeller.QueryResult;
import com.soundcloud.propeller.query.Query;
import com.soundcloud.propeller.rx.DatabaseScheduler;
import com.soundcloud.propeller.rx.RxResultMapper;
import rx.Observable;
import rx.Scheduler;

import javax.inject.Inject;
import java.util.Iterator;
import java.util.List;

public class PlaylistStorage {

    private final DatabaseScheduler scheduler;
    private final PropellerDatabase database;

    @Inject
    public PlaylistStorage(PropellerDatabase database, Scheduler scheduler) {
        this.database = database;
        this.scheduler = new DatabaseScheduler(database, scheduler);
    }

    public Observable<Urn> trackUrns(final Urn playlistUrn) {
        Query query = Query.from(Table.PLAYLIST_TRACKS.name)
                .select(TableColumns.PlaylistTracks.TRACK_ID)
                .whereEq(TableColumns.PlaylistTracks.PLAYLIST_ID, playlistUrn.getNumericId())
                .order(TableColumns.PlaylistTracks.POSITION, Query.ORDER_ASC);
        return scheduler.scheduleQuery(query).map(new TrackUrnMapper());
    }

    public List<PropertySet> backFillLikesStatus(List<PropertySet> input) {
        Query query = forLikes(input);

        final QueryResult result = database.query(query);
        final Iterator<CursorReader> iterator = result.iterator();
        for (int i = 0; i < input.size() && iterator.hasNext(); i++) {
            final PropertySet source = input.get(i);
            final boolean isLiked = iterator.next().getBoolean("is_liked");
            source.put(PlayableProperty.IS_LIKED, isLiked);
        }
        return input;
    }

    private Query forLikes(List<PropertySet> input) {
        return Query.apply(exists(Query.from(Table.COLLECTION_ITEMS.name)
                .whereEq(TableColumns.CollectionItems.COLLECTION_TYPE, CollectionStorage.CollectionItemTypes.LIKE)
                .whereEq(TableColumns.CollectionItems.RESOURCE_TYPE, TableColumns.Sounds.TYPE_PLAYLIST)
                .whereIn(TableColumns.CollectionItems.ITEM_ID, getPlaylistIds(input)))).as("is_liked");
    }

    private static final class TrackUrnMapper extends RxResultMapper<Urn> {
        @Override
        public Urn map(CursorReader cursorReader) {
            return Urn.forTrack(cursorReader.getLong(TableColumns.PlaylistTracks.TRACK_ID));
        }
    }

    private List<Long> getPlaylistIds(List<PropertySet> playlists) {
        return Lists.transform(playlists, new Function<PropertySet, Long>() {
            @Override
            public Long apply(PropertySet input) {
                return input.get(PlaylistProperty.URN).getNumericId();
            }
        });
    }
}
