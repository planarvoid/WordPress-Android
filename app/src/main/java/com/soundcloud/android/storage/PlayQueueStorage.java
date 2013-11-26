package com.soundcloud.android.storage;

import com.soundcloud.android.SoundCloudApplication;
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
        ContentResolver resolver = SoundCloudApplication.instance.getContentResolver();
        mPlayQueueDAO = new PlayQueueDAO(resolver);
    }

    public void clearState() {
        mPlayQueueDAO.deleteAll();
    }

    public Observable<Collection<PlayQueueItem>> storeCollectionAsync(final Collection<PlayQueueItem> playQueueItems) {
        return schedule(Observable.create(new Observable.OnSubscribeFunc<Collection<PlayQueueItem>>() {
            @Override
            public Subscription onSubscribe(Observer<? super Collection<PlayQueueItem>> observer) {
                mPlayQueueDAO.createCollection(playQueueItems);
                observer.onNext(playQueueItems);
                observer.onCompleted();
                return Subscriptions.empty();
            }
        }));
    }

    public Observable<List<PlayQueueItem>> getPlayQueueItemsAsync(){
        return schedule(Observable.create(new Observable.OnSubscribeFunc<List<PlayQueueItem>>() {
            @Override
            public Subscription onSubscribe(Observer<? super List<PlayQueueItem>> t1) {
                t1.onNext(mPlayQueueDAO.queryAll());
                t1.onCompleted();
                return Subscriptions.empty();
            }
        }));
    }


//    public Observable<PlayQueue> getPlayQueueAsync(final int playPosition, final PlaySessionSource playSessionSource) {
//        return schedule(Observable.create(new Observable.OnSubscribeFunc<PlayQueue>() {
//            @Override
//            public Subscription onSubscribe(Observer<? super PlayQueue> observer) {
//                final BooleanSubscription subscription = new BooleanSubscription();
//                Cursor cursor = mResolver.query(Content.PLAY_QUEUE.uri, new String[]{DBHelper.PlayQueue.TRACK_ID}, null, null, null);
//                if (!subscription.isUnsubscribed()) {
//                    if (cursor == null) {
//                        observer.onCompleted();
//                    } else {
//                        List<Long> trackIds = Lists.newArrayListWithExpectedSize(cursor.getCount());
//                        try {
//                            while (cursor.moveToNext()) {
//                                trackIds.add(cursor.getLong(cursor.getColumnIndex(DBHelper.PlayQueue.TRACK_ID)));
//                            }
//                            PlayQueue playQueue = new PlayQueue(trackIds, playPosition, playSessionSource);
//
//                            // TODO recreate the TrackSourceInfo objects
//
//                            observer.onNext(playQueue);
//                            observer.onCompleted();
//                        } finally {
//                            cursor.close();
//                        }
//                    }
//                }
//                return subscription;
//            }
//        }));
//    }
}
