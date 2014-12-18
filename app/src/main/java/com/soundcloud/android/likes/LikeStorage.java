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


public class LikeStorage {

    private final DatabaseScheduler scheduler;
    private final PropellerDatabase propellerDatabase;

    public LikeStorage(DatabaseScheduler scheduler, PropellerDatabase propellerDatabase) {
        this.scheduler = scheduler;
        this.propellerDatabase = propellerDatabase;
    }

    public Observable<PropertySet> trackLikes() {
        final Query query = Query.from(Table.Likes.name())
                .whereEq(TableColumns.Likes._TYPE, TableColumns.Sounds.TYPE_TRACK)
                .whereNull(TableColumns.Likes.REMOVED_AT);
        return scheduler.scheduleQuery(query).map(new LikeMapper());
    }

    private static class LikeMapper extends RxResultMapper<PropertySet> {
        @Override
        public PropertySet map(CursorReader cursorReader) {
            final PropertySet propertySet = PropertySet.create(cursorReader.getColumnCount());
            if (cursorReader.getInt(TableColumns.Likes._TYPE) == TableColumns.Sounds.TYPE_TRACK) {
                propertySet.put(LikeProperty.TARGET_URN, Urn.forTrack(cursorReader.getLong(TableColumns.Likes._ID)));
            } else if (cursorReader.getInt(TableColumns.Likes._TYPE) == TableColumns.Sounds.TYPE_PLAYLIST) {
                propertySet.put(LikeProperty.TARGET_URN, Urn.forPlaylist(cursorReader.getLong(TableColumns.Likes._ID)));
            }

            propertySet.put(LikeProperty.CREATED_AT, cursorReader.getDateFromTimestamp(TableColumns.Likes.CREATED_AT));
            if (!cursorReader.isNull(TableColumns.Likes.REMOVED_AT)) {
                propertySet.put(LikeProperty.REMOVED_AT, cursorReader.getDateFromTimestamp(TableColumns.Likes.REMOVED_AT ));
            }

            return propertySet;
        }
    }
}
