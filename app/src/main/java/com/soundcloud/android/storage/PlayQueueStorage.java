package com.soundcloud.android.storage;

import com.soundcloud.android.model.PlayQueueItem;
import com.soundcloud.android.rx.ScSchedulers;
import com.soundcloud.android.rx.ScheduledOperations;
import rx.Observable;
import rx.Subscriber;

import android.content.ContentResolver;

import javax.inject.Inject;
import java.util.Collection;
import java.util.List;

public class PlayQueueStorage extends ScheduledOperations {

    private final PlayQueueDAO playQueueDAO;

    @Inject
    public PlayQueueStorage(ContentResolver contentResolver) {
        super(ScSchedulers.STORAGE_SCHEDULER);
        playQueueDAO = new PlayQueueDAO(contentResolver);
    }

    public void clearState() {
        playQueueDAO.deleteAll();
    }

    public Observable<Collection<PlayQueueItem>> storeCollectionAsync(final Collection<PlayQueueItem> playQueueItems) {
        return schedule(Observable.create(new Observable.OnSubscribe<Collection<PlayQueueItem>>() {
            @Override
            public void call(Subscriber<? super Collection<PlayQueueItem>> observer) {
                observer.onNext(storeCollection(playQueueItems));
                observer.onCompleted();
            }
        }));
    }

    public Collection<PlayQueueItem> storeCollection(Collection<PlayQueueItem> playQueueItems) {
        playQueueDAO.createCollection(playQueueItems);
        return playQueueItems;
    }

    public Observable<List<PlayQueueItem>> getPlayQueueItemsAsync(){
        return schedule(Observable.create(new Observable.OnSubscribe<List<PlayQueueItem>>() {
            @Override
            public void call(Subscriber<? super List<PlayQueueItem>> t1) {
                t1.onNext(getPlayQueueItems());
                t1.onCompleted();
            }
        }));
    }

    public List<PlayQueueItem> getPlayQueueItems() {
        return playQueueDAO.queryAll();
    }
}
