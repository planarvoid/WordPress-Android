package com.soundcloud.android.likes;

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

import javax.inject.Inject;
import java.util.List;

public class LikeStorage {

    private final DatabaseScheduler scheduler;
    private final PropellerDatabase database;

    @Inject
    public LikeStorage(DatabaseScheduler scheduler) {
        this.scheduler = scheduler;
        this.database = scheduler.database();
    }

    public Observable<PropertySet> trackLikes() {
        return scheduler.scheduleQuery(Query.from(Table.Likes.name())
                .whereEq(TableColumns.Likes._TYPE, TableColumns.Sounds.TYPE_TRACK)
                .order(TableColumns.Likes.CREATED_AT, Query.ORDER_DESC)
                .whereNull(TableColumns.Likes.REMOVED_AT))
                .map(new LikeMapper());
    }

    public List<PropertySet> loadTrackLikes() {
        return database.query(Query.from(Table.Likes.name())
                .whereEq(TableColumns.Likes._TYPE, TableColumns.Sounds.TYPE_TRACK)
                .order(TableColumns.Likes.CREATED_AT, Query.ORDER_DESC)
                .whereNull(TableColumns.Likes.REMOVED_AT))
                .toList(new LikeMapper());
    }

    public List<PropertySet> loadPlaylistLikes() {
        return database.query(Query.from(Table.Likes.name())
                .whereEq(TableColumns.Likes._TYPE, TableColumns.Sounds.TYPE_PLAYLIST)
                .whereNull(TableColumns.Likes.REMOVED_AT))
                .toList(new LikeMapper());
    }

    public List<PropertySet> loadTrackLikesPendingRemoval() {
        return database.query(Query.from(Table.Likes.name())
                .whereEq(TableColumns.Likes._TYPE, TableColumns.Sounds.TYPE_TRACK)
                .whereNotNull(TableColumns.Likes.REMOVED_AT))
                .toList(new LikeMapper());
    }

    public List<PropertySet> loadPlaylistLikesPendingRemoval() {
        return database.query(Query.from(Table.Likes.name())
                .whereEq(TableColumns.Likes._TYPE, TableColumns.Sounds.TYPE_PLAYLIST)
                .whereNotNull(TableColumns.Likes.REMOVED_AT))
                .toList(new LikeMapper());
    }

    private static class LikeMapper extends RxResultMapper<PropertySet> {
        @Override
        public PropertySet map(CursorReader cursorReader) {
            final PropertySet like = PropertySet.create(cursorReader.getColumnCount());
            if (cursorReader.getInt(TableColumns.Likes._TYPE) == TableColumns.Sounds.TYPE_TRACK) {
                like.put(LikeProperty.TARGET_URN, Urn.forTrack(cursorReader.getLong(TableColumns.Likes._ID)));
            } else if (cursorReader.getInt(TableColumns.Likes._TYPE) == TableColumns.Sounds.TYPE_PLAYLIST) {
                like.put(LikeProperty.TARGET_URN, Urn.forPlaylist(cursorReader.getLong(TableColumns.Likes._ID)));
            }

            like.put(LikeProperty.CREATED_AT, cursorReader.getDateFromTimestamp(TableColumns.Likes.CREATED_AT));
            if (cursorReader.isNotNull(TableColumns.Likes.REMOVED_AT)) {
                like.put(LikeProperty.REMOVED_AT, cursorReader.getDateFromTimestamp(TableColumns.Likes.REMOVED_AT));
            }

            return like;
        }
    }
}
