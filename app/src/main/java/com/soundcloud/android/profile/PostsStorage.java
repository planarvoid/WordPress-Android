package com.soundcloud.android.profile;

import static com.soundcloud.android.storage.Table.Posts;
import static com.soundcloud.android.storage.Table.SoundView;
import static com.soundcloud.propeller.query.Field.field;
import static com.soundcloud.propeller.query.Query.Order.DESC;
import static com.soundcloud.propeller.query.Query.on;

import com.soundcloud.android.api.model.Sharing;
import com.soundcloud.android.model.PostProperty;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.storage.TableColumns;
import com.soundcloud.android.tracks.TrackProperty;
import com.soundcloud.java.collections.PropertySet;
import com.soundcloud.propeller.CursorReader;
import com.soundcloud.propeller.query.Query;
import com.soundcloud.propeller.rx.PropellerRx;
import com.soundcloud.propeller.rx.RxResultMapper;
import rx.Observable;

import android.provider.BaseColumns;

import javax.inject.Inject;

public class PostsStorage {

    private final PropellerRx propellerRx;

    @Inject
    public PostsStorage(PropellerRx propellerRx) {
        this.propellerRx = propellerRx;
    }

    public Observable<PropertySet> loadLastPublicPostedTrack() {
        return propellerRx.query(buildQueryForLastPublicPostedTrack()).map(new LastPostedTrackMapper());
    }

    private Query buildQueryForLastPublicPostedTrack() {
        return Query.from(Posts.name())
                    .select(
                            field(SoundView.field(TableColumns.SoundView._ID)).as(TableColumns.SoundView._ID),
                            field(Posts.field(TableColumns.Posts.CREATED_AT)).as(TableColumns.Posts.CREATED_AT),
                            field(SoundView.field(TableColumns.SoundView.PERMALINK_URL)).as(TableColumns.SoundView.PERMALINK_URL))
                    .innerJoin(SoundView.name(),
                               on(SoundView.field(TableColumns.SoundView._ID),
                                  Posts.field(TableColumns.Posts.TARGET_ID))
                                       .whereEq(SoundView.field(TableColumns.SoundView._TYPE),
                                                Posts.field(TableColumns.Posts.TARGET_TYPE)))
                    .whereEq(SoundView.field(TableColumns.SoundView._TYPE), TableColumns.Sounds.TYPE_TRACK)
                    .whereEq(Posts.field(TableColumns.Posts.TYPE), TableColumns.Posts.TYPE_POST)
                    .whereNotEq(SoundView.field(TableColumns.SoundView.SHARING), Sharing.PRIVATE.value())
                    .groupBy(SoundView.field(TableColumns.SoundView._ID) + "," + SoundView.field(TableColumns.SoundView._TYPE))
                    .order(Posts.field(TableColumns.Posts.CREATED_AT), DESC)
                    .limit(1);
    }

    private class LastPostedTrackMapper extends RxResultMapper<PropertySet> {
        @Override
        public PropertySet map(CursorReader cursorReader) {
            final PropertySet propertySet = PropertySet.create(cursorReader.getColumnCount());
            propertySet.put(TrackProperty.URN, Urn.forTrack(cursorReader.getLong(BaseColumns._ID)));
            propertySet.put(PostProperty.CREATED_AT, cursorReader.getDateFromTimestamp(TableColumns.Posts.CREATED_AT));
            propertySet.put(TrackProperty.PERMALINK_URL, cursorReader.getString(TableColumns.SoundView.PERMALINK_URL));
            return propertySet;
        }
    }

}