package com.soundcloud.android.playback;

import com.soundcloud.android.model.Urn;
import com.soundcloud.android.storage.Tables;
import com.soundcloud.propeller.ChangeResult;
import com.soundcloud.propeller.ContentValuesBuilder;
import com.soundcloud.propeller.CursorReader;
import com.soundcloud.propeller.TxnResult;
import com.soundcloud.propeller.query.Query;
import com.soundcloud.propeller.rx.PropellerRx;
import com.soundcloud.propeller.rx.RxResultMapper;
import com.soundcloud.propeller.schema.Table;
import rx.Observable;
import rx.functions.Func1;

import android.content.ContentValues;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;

class PlayQueueStorage {

    private static final Table TABLE = Tables.PlayQueue.TABLE;

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
                final ContentValuesBuilder valuesBuilder = ContentValuesBuilder.values(4)
                        .put(Tables.PlayQueue.TRACK_ID, item.getTrackUrn().getNumericId())
                        .put(Tables.PlayQueue.SOURCE, item.getSource())
                        .put(Tables.PlayQueue.SOURCE_VERSION, item.getSourceVersion());

                if (item.getReposter().isUser()){
                    valuesBuilder.put(Tables.PlayQueue.REPOSTER_ID, item.getReposter().getNumericId());
                }
                newItems.add(valuesBuilder.get());
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
                final PlayQueueItem playQueueItem = PlayQueueItem.fromTrack(
                        Urn.forTrack(reader.getLong(Tables.PlayQueue.TRACK_ID)),
                        hasReposter(reader) ? Urn.forUser(reader.getLong(Tables.PlayQueue.REPOSTER_ID)) : Urn.NOT_SET,
                        reader.getString(Tables.PlayQueue.SOURCE),
                        reader.getString(Tables.PlayQueue.SOURCE_VERSION)
                );
                return playQueueItem;
            }
        });
    }

    private boolean hasReposter(CursorReader reader) {
        return reader.isNotNull(Tables.PlayQueue.REPOSTER_ID) && reader.getLong(Tables.PlayQueue.REPOSTER_ID) > 0;
    }
}
