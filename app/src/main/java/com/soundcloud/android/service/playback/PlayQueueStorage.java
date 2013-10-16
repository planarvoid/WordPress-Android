package com.soundcloud.android.service.playback;

import com.google.common.collect.Lists;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.dao.Storage;
import com.soundcloud.android.model.Playable;
import com.soundcloud.android.provider.Content;
import com.soundcloud.android.provider.DBHelper;
import com.soundcloud.android.rx.ScheduledOperations;
import com.soundcloud.android.utils.Log;
import rx.Observable;
import rx.Observer;
import rx.Subscription;
import rx.subscriptions.BooleanSubscription;
import rx.subscriptions.Subscriptions;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;

import java.util.Collections;
import java.util.List;

public class PlayQueueStorage extends ScheduledOperations implements Storage<PlayQueue> {

    private final ContentResolver mResolver;

    public PlayQueueStorage() {
        mResolver = SoundCloudApplication.instance.getContentResolver();
    }

    public void clearState() {
        mResolver.delete(Content.PLAY_QUEUE.uri, null, null);
    }

    public int getPlayQueuePositionFromUri(Uri collectionUri, long itemId) {
        Cursor cursor = mResolver.query(collectionUri,
                new String[]{DBHelper.SoundAssociationView._ID},
                DBHelper.SoundAssociationView._TYPE + " = ?",
                new String[]{String.valueOf(Playable.DB_TYPE_TRACK)},
                null);

        int position = -1;
        if (cursor != null) {
            while (cursor.moveToNext() && position == -1) {
                if (cursor.getLong(0) == itemId) position = cursor.getPosition();
            }
            cursor.close();
        }
        return position;
    }

    @Override
    public PlayQueue store(PlayQueue playQueue) {
        final List<Long> trackIds = playQueue.getTrackIds();
        ContentValues[] contentValues = new ContentValues[trackIds.size()];
        for (int i = 0; i < trackIds.size(); i++) {
            ContentValues cv = new ContentValues();
            cv.put(DBHelper.PlayQueue.POSITION, i);
            cv.put(DBHelper.PlayQueue.TRACK_ID, trackIds.get(i));
            contentValues[i] = cv;
        }
        int trackCount = mResolver.bulkInsert(Content.PLAY_QUEUE.uri, contentValues);
        Log.d(CloudPlaybackService.TAG, trackCount + " tracks saved from playqueue");
        return playQueue;
    }

    @Override
    public Observable<PlayQueue> storeAsync(final PlayQueue playQueue) {
        return schedule(Observable.create(new Observable.OnSubscribeFunc<PlayQueue>() {
            @Override
            public Subscription onSubscribe(Observer<? super PlayQueue> observer) {
                observer.onNext(store(playQueue));
                observer.onCompleted();
                return Subscriptions.empty();
            }
        }));
    }

    /**
     * TODO : any way to be more DRY and combine with {@link com.soundcloud.android.dao.TrackStorage#getTrackIdsForUriAsync(android.net.Uri)}
      */

    public Observable<List<Long>> getTrackIds() {
        return schedule(Observable.create(new Observable.OnSubscribeFunc<List<Long>>() {
            @Override
            public Subscription onSubscribe(Observer<? super List<Long>> observer) {
                final BooleanSubscription subscription = new BooleanSubscription();
                Cursor cursor = mResolver.query(Content.PLAY_QUEUE.uri, new String[]{DBHelper.PlayQueue.TRACK_ID}, null, null, null);
                if (!subscription.isUnsubscribed()) {
                    if (cursor == null) {
                        observer.onNext(Collections.<Long>emptyList());
                        observer.onCompleted();

                    } else {
                        List<Long> newQueue = Lists.newArrayListWithExpectedSize(cursor.getCount());
                        try {
                            while (cursor.moveToNext()) {
                                newQueue.add(cursor.getLong(cursor.getColumnIndex(DBHelper.PlayQueue.TRACK_ID)));
                            }
                            observer.onNext(newQueue);
                            observer.onCompleted();

                        } finally {
                            cursor.close();
                        }
                    }
                }
                return subscription;
            }
        }));
    }
}
