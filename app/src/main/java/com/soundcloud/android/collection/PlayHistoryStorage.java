package com.soundcloud.android.collection;

import static com.soundcloud.android.tracks.TrackItemMapper.BASE_TRACK_FIELDS;
import static com.soundcloud.propeller.query.Field.field;

import com.soundcloud.android.model.Urn;
import com.soundcloud.android.storage.Table;
import com.soundcloud.android.storage.TableColumns.SoundView;
import com.soundcloud.android.storage.TableColumns.Sounds;
import com.soundcloud.android.storage.Tables.PlayHistory;
import com.soundcloud.android.tracks.TrackItem;
import com.soundcloud.android.tracks.TrackItemMapper;
import com.soundcloud.propeller.CursorReader;
import com.soundcloud.propeller.PropellerDatabase;
import com.soundcloud.propeller.query.Filter;
import com.soundcloud.propeller.query.Query;
import com.soundcloud.propeller.rx.PropellerRx;
import rx.Observable;
import rx.functions.Func1;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;

public class PlayHistoryStorage {

    private final PropellerDatabase database;
    private final PropellerRx rxDatabase;

    @Inject
    public PlayHistoryStorage(PropellerDatabase database) {
        this.database = database;
        this.rxDatabase = new PropellerRx(database);
    }

    Observable<TrackItem> fetchPlayHistory(int limit) {
        return rxDatabase.query(fetchQuery(limit))
                .map(new TrackItemMapper())
                .map(TrackItem.fromPropertySet());
    }

    Observable<Urn> fetchPlayHistoryForPlayback() {
        return rxDatabase.query(fetchForPlaybackQuery()).map(new Func1<CursorReader, Urn>() {
            @Override
            public Urn call(CursorReader cursorReader) {
                return Urn.forTrack(cursorReader.getLong(PlayHistory.TRACK_ID.name()));
            }
        });
    }

    private Query fetchQuery(int limit) {
        List<Object> fields = new ArrayList<>(BASE_TRACK_FIELDS.size() + 2);
        fields.addAll(BASE_TRACK_FIELDS);
        // These fields are required by TrackItemMapper but not needed by the ItemRenderer
        // field("0").as(...) sets the field to false
        fields.add(field("0").as(SoundView.USER_LIKE));
        fields.add(field("0").as(SoundView.USER_REPOST));

        return Query.from(PlayHistory.TABLE)
                .select(fields.toArray())
                .innerJoin(Table.SoundView, Filter.filter()
                        .whereEq(Sounds._ID, PlayHistory.TRACK_ID)
                        .whereEq(Sounds._TYPE, Sounds.TYPE_TRACK))
                .order(PlayHistory.TIMESTAMP, Query.Order.DESC)
                .limit(limit);
    }

    public void clear() {
        database.delete(PlayHistory.TABLE);
    }

    private Query fetchForPlaybackQuery() {
        return Query.from(PlayHistory.TABLE.name())
                .select("DISTINCT " + PlayHistory.TRACK_ID.name())
                .order(PlayHistory.TIMESTAMP, Query.Order.DESC);
    }

}
