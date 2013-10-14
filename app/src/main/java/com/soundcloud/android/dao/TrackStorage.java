package com.soundcloud.android.dao;

import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.model.Playable;
import com.soundcloud.android.model.Track;
import com.soundcloud.android.provider.Content;
import com.soundcloud.android.provider.DBHelper;
import com.soundcloud.android.rx.ScheduledOperations;
import com.soundcloud.android.service.playback.PlayQueueManager;
import rx.Observable;
import rx.Observer;
import rx.Subscription;
import rx.subscriptions.Subscriptions;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.util.Log;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class TrackStorage extends ScheduledOperations implements Storage<Track> {
    private TrackDAO mTrackDAO;
    private final ContentResolver mResolver;

    public TrackStorage() {
        mResolver = SoundCloudApplication.instance.getContentResolver();
        mTrackDAO = new TrackDAO(mResolver);
    }

    public boolean markTrackAsPlayed(Track track) {
        ContentValues contentValues = new ContentValues();
        contentValues.put(DBHelper.TrackMetadata._ID, track.getId());
        return mResolver.insert(Content.TRACK_PLAYS.uri, contentValues) != null;
    }

    @Override
    public Observable<Track> create(final Track track) {
        return schedule(Observable.create(new Observable.OnSubscribeFunc<Track>() {
            @Override
            public Subscription onSubscribe(Observer<? super Track> observer) {
                mTrackDAO.create(track);
                observer.onNext(track);
                observer.onCompleted();
                return Subscriptions.empty();
            }
        }));
    }

    public long createOrUpdate(Track track) {
        return mTrackDAO.createOrUpdate(track);
    }

    public Observable<Track> getTrack(final long id) {
        return schedule(Observable.create(new Observable.OnSubscribeFunc<Track>() {
            @Override
            public Subscription onSubscribe(Observer<? super Track> observer) {
                observer.onNext(mTrackDAO.queryById(id));
                observer.onCompleted();
                return Subscriptions.empty();
            }
        }));
    }

    public Track getTrack(Uri uri) {
        return mTrackDAO.queryByUri(uri);
    }

    public List<Track> getTracksForUri(Uri uri) {
        Cursor cursor;
        try {
            cursor = mResolver.query(uri, null, DBHelper.SoundView._TYPE + " = ?",
                    new String[]{String.valueOf(Playable.DB_TYPE_TRACK)}, null);
        } catch (IllegalArgumentException e) {
            // in case we load a deprecated URI, just don't load the playlist
            Log.e(PlayQueueManager.class.getSimpleName(), "Tried to load an invalid uri " + uri);
            return Collections.emptyList();
        }
        if (cursor != null) {
            boolean isActivityCursor = Content.match(uri).isActivitiesItem();
            List<Track> newQueue = new ArrayList<Track>(cursor.getCount());
            while (cursor.moveToNext()) {
                final Track trackFromCursor = isActivityCursor ?
                        SoundCloudApplication.MODEL_MANAGER.getCachedTrackFromCursor(cursor, DBHelper.ActivityView.SOUND_ID) :
                        SoundCloudApplication.MODEL_MANAGER.getCachedTrackFromCursor(cursor);
                newQueue.add(trackFromCursor);
            }
            cursor.close();
            return newQueue;
        } else {
            return Collections.emptyList();
        }
    }
}


