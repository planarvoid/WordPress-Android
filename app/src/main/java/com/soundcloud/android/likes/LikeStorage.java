package com.soundcloud.android.likes;

import com.soundcloud.android.model.Urn;
import com.soundcloud.android.storage.Table;
import com.soundcloud.android.storage.TableColumns;
import com.soundcloud.propeller.CursorReader;
import com.soundcloud.propeller.PropertySet;
import com.soundcloud.propeller.query.Query;
import com.soundcloud.propeller.rx.DatabaseScheduler;
import com.soundcloud.propeller.rx.RxResultMapper;
import rx.Observable;

import javax.inject.Inject;

public class LikeStorage {

    private final DatabaseScheduler scheduler;

    @Inject
    public LikeStorage(DatabaseScheduler scheduler) {
        this.scheduler = scheduler;
    }

    public Observable<PropertySet> trackLikes() {
        return scheduler.scheduleQuery(Query.from(Table.Likes.name())
                .whereEq(TableColumns.Likes._TYPE, TableColumns.Sounds.TYPE_TRACK)
                .order(TableColumns.Likes.CREATED_AT, Query.ORDER_DESC)
                .whereNull(TableColumns.Likes.REMOVED_AT))
                .map(new LikeMapper());
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
