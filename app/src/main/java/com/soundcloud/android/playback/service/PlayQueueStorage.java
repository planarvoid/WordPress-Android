package com.soundcloud.android.playback.service;

import com.soundcloud.android.storage.Table;
import com.soundcloud.android.storage.TableColumns;
import com.soundcloud.propeller.BulkInsertResult;
import com.soundcloud.propeller.ChangeResult;
import com.soundcloud.propeller.ContentValuesBuilder;
import com.soundcloud.propeller.CursorReader;
import com.soundcloud.propeller.Query;
import com.soundcloud.propeller.rx.DatabaseScheduler;
import com.soundcloud.propeller.rx.RxResultMapper;
import rx.Observable;
import rx.functions.Func1;

import android.content.ContentValues;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;

public class PlayQueueStorage {

    private static final String TABLE = Table.PLAY_QUEUE.name;

    private final DatabaseScheduler scheduler;

    @Inject
    public PlayQueueStorage(DatabaseScheduler scheduler) {
        this.scheduler = scheduler;
    }

    public Observable<ChangeResult> clearAsync() {
        return scheduler.scheduleTruncate(TABLE);
    }

    public Observable<BulkInsertResult> storeAsync(final PlayQueue playQueue) {
        final List<ContentValues> newItems = new ArrayList<ContentValues>(playQueue.size());
        for (PlayQueueItem item : playQueue) {
            newItems.add(ContentValuesBuilder.values(3)
                    .put(TableColumns.PlayQueue.TRACK_ID, item.getTrackId())
                    .put(TableColumns.PlayQueue.SOURCE, item.getSource())
                    .put(TableColumns.PlayQueue.SOURCE_VERSION, item.getSourceVersion())
                    .get());
        }

        return clearAsync().mergeMap(new Func1<ChangeResult, Observable<BulkInsertResult>>() {
            @Override
            public Observable<BulkInsertResult> call(ChangeResult changeResult) {
                return scheduler.scheduleInsert(TABLE, newItems);
            }
        });
    }

    public Observable<PlayQueueItem> loadAsync() {
        return scheduler.scheduleQuery(Query.from(TABLE)).map(new RxResultMapper<PlayQueueItem>() {
            @Override
            public PlayQueueItem map(CursorReader reader) {
                return PlayQueueItem.fromTrack(
                        reader.getLong(TableColumns.PlayQueue.TRACK_ID),
                        reader.getString(TableColumns.PlayQueue.SOURCE),
                        reader.getString(TableColumns.PlayQueue.SOURCE_VERSION)
                );
            }
        });
    }
}
