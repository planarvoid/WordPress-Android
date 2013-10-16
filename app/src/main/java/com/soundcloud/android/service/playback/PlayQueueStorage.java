package com.soundcloud.android.service.playback;

import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.dao.Storage;
import com.soundcloud.android.model.Playable;
import com.soundcloud.android.provider.Content;
import com.soundcloud.android.provider.DBHelper;
import com.soundcloud.android.rx.ScheduledOperations;
import rx.Observable;
import rx.Observer;
import rx.Subscription;
import rx.subscriptions.Subscriptions;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;

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
        mResolver.bulkInsert(Content.PLAY_QUEUE.uri, contentValues);
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
}
