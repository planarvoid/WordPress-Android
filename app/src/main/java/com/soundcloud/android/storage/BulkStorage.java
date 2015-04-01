package com.soundcloud.android.storage;

import com.soundcloud.android.api.legacy.model.PublicApiResource;
import com.soundcloud.android.rx.ScSchedulers;
import com.soundcloud.android.rx.ScheduledOperations;
import com.soundcloud.android.storage.provider.BulkInsertMap;
import rx.Observable;
import rx.Scheduler;
import rx.Subscriber;

import android.content.ContentResolver;

import javax.inject.Inject;

@Deprecated
public class BulkStorage extends ScheduledOperations {

    private final ContentResolver contentResolver;

    @Inject
    public BulkStorage(ContentResolver contentResolver) {
        this(ScSchedulers.HIGH_PRIO_SCHEDULER, contentResolver);
    }

    BulkStorage(Scheduler scheduler, ContentResolver contentResolver) {
        super(scheduler);
        this.contentResolver = contentResolver;
    }

    public void bulkInsert(Iterable<? extends PublicApiResource> resources) {
        BulkInsertMap insertMap = new BulkInsertMap();
        for (PublicApiResource r : resources) {
            r.putFullContentValues(insertMap);
        }
        insertMap.insert(contentResolver);
    }

    public <T extends Iterable<? extends PublicApiResource>> Observable<T> bulkInsertAsync(final T resources) {
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
