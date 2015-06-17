package com.soundcloud.android.playback.service;

import com.soundcloud.android.model.Urn;
import com.soundcloud.android.storage.Table;
import com.soundcloud.android.storage.TableColumns;
import com.soundcloud.propeller.ChangeResult;
import com.soundcloud.propeller.ContentValuesBuilder;
import com.soundcloud.propeller.CursorReader;
import com.soundcloud.propeller.TxnResult;
import com.soundcloud.propeller.query.Query;
import com.soundcloud.propeller.rx.PropellerRx;
import com.soundcloud.propeller.rx.RxResultMapper;
import rx.Observable;
import rx.functions.Func1;

import android.content.ContentValues;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;

public class PlayQueueStorage {

    private static final Table TABLE = Table.PlayQueue;

    private final PropellerRx propellerRx;

    @Inject
    public PlayQueueStorage(PropellerRx propellerRx) {
        this.propellerRx = propellerRx;
    }

    public Observable<ChangeResult> clearAsync() {
        return propellerRx.truncate(TABLE);
    }

    public Observable<TxnResult> storeAsync(final PlayQueue playQueue) {
        final List<ContentValues> newItems = new ArrayList<>(playQueue.size());
        for (PlayQueueItem item : playQueue) {
            if (item.shouldPersist()) {
                newItems.add(ContentValuesBuilder.values(3)
                        .put(TableColumns.PlayQueue.TRACK_ID, item.getTrackUrn().getNumericId())
                        .put(TableColumns.PlayQueue.SOURCE, item.getSource())
                        .put(TableColumns.PlayQueue.SOURCE_VERSION, item.getSourceVersion())
                        .get());
            }
        }

        return clearAsync().flatMap(new Func1<ChangeResult, Observable<TxnResult>>() {
            @Override
            public Observable<TxnResult> call(ChangeResult changeResult) {
                return propellerRx.bulkInsert(TABLE, newItems);
            }
        });
    }

    public Observable<PlayQueueItem> loadAsync() {
        return propellerRx.query(Query.from(TABLE.name())).map(new RxResultMapper<PlayQueueItem>() {
            @Override
            public PlayQueueItem map(CursorReader reader) {
                return PlayQueueItem.fromTrack(
                        Urn.forTrack(reader.getLong(TableColumns.PlayQueue.TRACK_ID)),
                        reader.getString(TableColumns.PlayQueue.SOURCE),
                        reader.getString(TableColumns.PlayQueue.SOURCE_VERSION)
                );
            }
        });
    }
}
