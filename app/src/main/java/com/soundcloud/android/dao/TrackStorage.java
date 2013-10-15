package com.soundcloud.android.dao;

import com.google.common.collect.Lists;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.model.Playable;
import com.soundcloud.android.model.ScModelManager;
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

import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class TrackStorage extends ScheduledOperations implements Storage<Track> {
    private TrackDAO mTrackDAO;
    private ContentResolver mResolver;
    private ScModelManager mModelManager;

    public TrackStorage() {
        this(SoundCloudApplication.instance.getContentResolver(),
                new TrackDAO(SoundCloudApplication.instance.getContentResolver()),
                SoundCloudApplication.MODEL_MANAGER);
    }

    public TrackStorage(ContentResolver contentResolver, TrackDAO trackDAO, ScModelManager modelManager){
        mResolver = contentResolver;
        mTrackDAO = trackDAO;
        mModelManager = modelManager;
    }

    public boolean markTrackAsPlayed(Track track) {
        ContentValues contentValues = new ContentValues();
        contentValues.put(DBHelper.TrackMetadata._ID, track.getId());
        return mResolver.insert(Content.TRACK_PLAYS.uri, contentValues) != null;
    }

    @Override
    public Track store(Track track) {
        mTrackDAO.create(track);
        return track;
    }

    @Override
    public Observable<Track> storeAsync(final Track track) {
        return schedule(Observable.create(new Observable.OnSubscribeFunc<Track>() {
            @Override
            public Subscription onSubscribe(Observer<? super Track> observer) {
                observer.onNext(store(track));
                observer.onCompleted();
                return Subscriptions.empty();
            }
        }));
    }

    public Observable<Collection<Track>> storeCollectionAsync(final Collection<Track> tracks) {
        return schedule(Observable.create(new Observable.OnSubscribeFunc<Collection<Track>>() {
            @Override
            public Subscription onSubscribe(Observer<? super Collection<Track>> observer) {
                storeCollection(tracks);
                observer.onNext(tracks);
                observer.onCompleted();
                return Subscriptions.empty();
            }
        }));
    }

    private int storeCollection(Collection<Track> tracks) {
        return mTrackDAO.createCollection(tracks);
    }


    public long createOrUpdate(Track track) {
        return mTrackDAO.createOrUpdate(track);
    }

    public Observable<Track> getTrack(final long id) {
        return schedule(Observable.create(new Observable.OnSubscribeFunc<Track>() {
            @Override
            public Subscription onSubscribe(Observer<? super Track> observer) {
                final Track cachedTrack = mModelManager.getCachedTrack(id);
                if (cachedTrack != null){
                    observer.onNext(cachedTrack);
                } else {
                    observer.onNext(mTrackDAO.queryById(id));
                }
                observer.onCompleted();
                return Subscriptions.empty();
            }
        }));
    }

    public Track getTrack(Uri uri) {
        return mTrackDAO.queryByUri(uri);
    }

    public List<Long> getTrackIdsForUri(Uri uri) {
        final boolean isActivityCursor = Content.match(uri).isActivitiesItem();
        final String idColumn = isActivityCursor ? DBHelper.ActivityView.SOUND_ID : DBHelper.SoundView._ID;

        Cursor cursor;
        try {

            cursor = mResolver.query(uri, new String[]{idColumn}, DBHelper.SoundView._TYPE + " = ?",
                    new String[]{String.valueOf(Playable.DB_TYPE_TRACK)}, null);
        } catch (IllegalArgumentException e) {
            // in case we load a deprecated URI, just don't load the playlist
            Log.e(PlayQueueManager.class.getSimpleName(), "Tried to load an invalid uri " + uri);
            return Collections.emptyList();
        }

        if (cursor != null) {
            List<Long> newQueue = Lists.newArrayListWithExpectedSize(cursor.getCount());
            while (cursor.moveToNext()) {
                newQueue.add(cursor.getLong(cursor.getColumnIndex(idColumn)));
            }
            cursor.close();
            return newQueue;
        } else {
            return Collections.emptyList();
        }
    }
}


