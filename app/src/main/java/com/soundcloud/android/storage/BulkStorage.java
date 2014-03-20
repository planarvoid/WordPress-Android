package com.soundcloud.android.storage;

import com.soundcloud.android.model.ScResource;
import com.soundcloud.android.rx.ScSchedulers;
import com.soundcloud.android.rx.ScheduledOperations;
import com.soundcloud.android.storage.provider.BulkInsertMap;
import rx.Observable;
import rx.Scheduler;
import rx.Subscriber;

import android.content.ContentResolver;

import javax.inject.Inject;

public class BulkStorage extends ScheduledOperations {

    private final ContentResolver mContentResolver;

    @Inject
    public BulkStorage(ContentResolver contentResolver) {
        this(ScSchedulers.STORAGE_SCHEDULER, contentResolver);
    }

    BulkStorage(Scheduler scheduler, ContentResolver contentResolver) {
        super(scheduler);
        mContentResolver = contentResolver;
    }

    public void bulkInsert(Iterable<? extends ScResource> resources) {
        BulkInsertMap insertMap = new BulkInsertMap();
        for (ScResource r : resources) {
            r.putFullContentValues(insertMap);
        }
        insertMap.insert(mContentResolver);
    }

    public <T extends Iterable<? extends ScResource>> Observable<T> bulkInsertAsync(final T resources) {
        return schedule(Observable.create(new Observable.OnSubscribe<T>() {
            @Override
            public void call(Subscriber<? super T> subscriber) {
                bulkInsert(resources);
                subscriber.onNext(resources);
                subscriber.onCompleted();
            }
        }));
    }
}
