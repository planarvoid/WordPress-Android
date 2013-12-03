package com.soundcloud.android.storage;

import com.soundcloud.android.model.PlayQueueItem;
import com.soundcloud.android.rx.ScSchedulers;
import com.soundcloud.android.rx.ScheduledOperations;
import rx.Observable;
import rx.Observer;
import rx.Subscription;
import rx.subscriptions.Subscriptions;

import android.content.ContentResolver;

import javax.inject.Inject;
import java.util.Collection;
import java.util.List;

public class PlayQueueStorage extends ScheduledOperations {

    private final PlayQueueDAO mPlayQueueDAO;

    @Inject
    public PlayQueueStorage(ContentResolver contentResolver) {
        super(ScSchedulers.STORAGE_SCHEDULER);
        mPlayQueueDAO = new PlayQueueDAO(contentResolver);
    }

    public void clearState() {
        mPlayQueueDAO.deleteAll();
    }

    public Observable<Collection<PlayQueueItem>> storeCollectionAsync(final Collection<PlayQueueItem> playQueueItems) {
        return schedule(Observable.create(new Observable.OnSubscribeFunc<Collection<PlayQueueItem>>() {
            @Override
            public Subscription onSubscribe(Observer<? super Collection<PlayQueueItem>> observer) {
                observer.onNext(storeCollection(playQueueItems));
                observer.onCompleted();
                return Subscriptions.empty();
            }
        }));
    }

    public Collection<PlayQueueItem> storeCollection(Collection<PlayQueueItem> playQueueItems) {
        mPlayQueueDAO.createCollection(playQueueItems);
        return playQueueItems;
    }

    public Observable<List<PlayQueueItem>> getPlayQueueItemsAsync(){
        return schedule(Observable.create(new Observable.OnSubscribeFunc<List<PlayQueueItem>>() {
            @Override
            public Subscription onSubscribe(Observer<? super List<PlayQueueItem>> t1) {
                t1.onNext(getPlayQueueItems());
                t1.onCompleted();
                return Subscriptions.empty();
            }
        }));
    }

    public List<PlayQueueItem> getPlayQueueItems() {
        return mPlayQueueDAO.queryAll();
    }
}
