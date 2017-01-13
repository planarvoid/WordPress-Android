package com.soundcloud.android.profile;

import static com.soundcloud.android.storage.Table.SoundView;
import static com.soundcloud.propeller.query.Field.field;
import static com.soundcloud.propeller.query.Query.Order.DESC;
import static com.soundcloud.propeller.query.Query.on;

import com.soundcloud.android.api.model.Sharing;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.storage.TableColumns;
import com.soundcloud.android.storage.Tables;
import com.soundcloud.android.storage.Tables.Posts;
import com.soundcloud.propeller.CursorReader;
import com.soundcloud.propeller.query.Query;
import com.soundcloud.propeller.rx.PropellerRx;
import com.soundcloud.propeller.rx.RxResultMapper;
import rx.Observable;

import android.provider.BaseColumns;

import javax.inject.Inject;
import java.util.Date;

public class PostsStorage {

    private final PropellerRx propellerRx;

    @Inject
    public PostsStorage(PropellerRx propellerRx) {
        this.propellerRx = propellerRx;
    }

    public Observable<LastPostedTrack> loadLastPublicPostedTrack() {
        return propellerRx.query(buildQueryForLastPublicPostedTrack()).map(new LastPostedTrackMapper());
    }

    private Query buildQueryForLastPublicPostedTrack() {
        return Query.from(Posts.TABLE)
                    .select(
                            field(SoundView.field(TableColumns.SoundView._ID)).as(TableColumns.SoundView._ID),
                            Posts.CREATED_AT,
                            field(SoundView.field(TableColumns.SoundView.PERMALINK_URL)).as(TableColumns.SoundView.PERMALINK_URL))
                    .innerJoin(SoundView.name(),
                               on(SoundView.field(TableColumns.SoundView._ID),
                                  Posts.TARGET_ID.qualifiedName())
                                       .whereEq(SoundView.field(TableColumns.SoundView._TYPE),
                                                Posts.TARGET_TYPE.qualifiedName()))
                    .whereEq(SoundView.field(TableColumns.SoundView._TYPE), Tables.Sounds.TYPE_TRACK)
                    .whereEq(Posts.TYPE, Tables.Posts.TYPE_POST)
                    .whereNotEq(SoundView.field(TableColumns.SoundView.SHARING), Sharing.PRIVATE.value())
                    .groupBy(SoundView.field(TableColumns.SoundView._ID) + "," + SoundView.field(TableColumns.SoundView._TYPE))
                    .order(Posts.CREATED_AT.qualifiedName(), DESC)
                    .limit(1);
    }

    private class LastPostedTrackMapper extends RxResultMapper<LastPostedTrack> {
        @Override
        public LastPostedTrack map(CursorReader cursorReader) {
            final Urn urn = Urn.forTrack(cursorReader.getLong(BaseColumns._ID));
            final Date createdAt = cursorReader.getDateFromTimestamp(Posts.CREATED_AT);
            final String permalinkUrl = cursorReader.getString(TableColumns.SoundView.PERMALINK_URL);
            return LastPostedTrack.create(urn, createdAt, permalinkUrl);
        }
    }

}
