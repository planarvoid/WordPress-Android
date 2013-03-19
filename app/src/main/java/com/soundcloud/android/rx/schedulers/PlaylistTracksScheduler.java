package com.soundcloud.android.rx.schedulers;

import com.soundcloud.android.dao.PlaylistStorage;
import com.soundcloud.android.model.Track;
import com.soundcloud.android.provider.Content;
import com.soundcloud.android.rx.observers.DetachableObserver;
import com.soundcloud.android.utils.UriUtils;
import rx.Observable;

import android.content.Context;
import android.net.Uri;

import java.util.Collections;
import java.util.List;


public class PlaylistTracksScheduler extends ReactiveScheduler<List<Track>> {

    private PlaylistStorage mStorage;

    public PlaylistTracksScheduler(Context context) {
        super(context);
        mStorage = new PlaylistStorage(context.getContentResolver());
    }

    @Override
    public Observable<List<Track>> loadFromLocalStorage(final long playlistId) {
        return Observable.create(newBackgroundJob(new ObservedRunnable<List<Track>>() {
            @Override
            protected void run(DetachableObserver<List<Track>> observer) {
                List<Track> tracks = mStorage.loadPlaylistTracks(playlistId);
                observer.onNext(tracks);
                observer.onCompleted();
            }
        }));
    }

}
