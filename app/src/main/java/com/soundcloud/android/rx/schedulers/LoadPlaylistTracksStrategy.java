package com.soundcloud.android.rx.schedulers;

import com.soundcloud.android.dao.PlaylistStorage;
import com.soundcloud.android.model.Track;
import com.soundcloud.android.rx.observers.DetachableObserver;
import com.soundcloud.android.utils.UriUtils;
import rx.Observable;

import android.content.Context;
import android.net.Uri;

import java.util.Collections;
import java.util.List;


public class LoadPlaylistTracksStrategy implements SyncManager.LocalStorageStrategy<List<Track>> {

    private PlaylistStorage mStorage;

    public LoadPlaylistTracksStrategy(Context context) {
        mStorage = new PlaylistStorage(context.getContentResolver());
    }

    @Override
    public Observable<List<Track>> loadFromContentUri(final Uri contentUri) {
        return Observable.create(ReactiveScheduler.newBackgroundJob(new ObservedRunnable<List<Track>>() {
            @Override
            protected void run(DetachableObserver<List<Track>> observer) {
                List<Track> tracks = mStorage.loadPlaylistTracks(UriUtils.getLastSegmentAsLong(contentUri));
                observer.onNext(tracks);
                observer.onCompleted();
            }
        }));
    }

}
