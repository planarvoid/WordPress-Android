package com.soundcloud.android.rx.schedulers;

import com.soundcloud.android.dao.PlaylistStorage;
import com.soundcloud.android.model.Playlist;
import com.soundcloud.android.rx.observers.DetachableObserver;
import rx.Observable;

import android.content.Context;
import android.net.Uri;

public class LoadPlaylistStrategy implements SyncManager.LocalStorageStrategy<Playlist> {

    private PlaylistStorage mStorage;

    public LoadPlaylistStrategy(Context context) {
        mStorage = new PlaylistStorage(context.getContentResolver());
    }

    @Override
    public Observable<Playlist> loadFromContentUri(final Uri contentUri) {
        return Observable.create(ReactiveScheduler.newBackgroundJob(new ObservedRunnable<Playlist>() {
            @Override
            protected void run(DetachableObserver<Playlist> observer) {
                Playlist playlist = mStorage.getPlaylistWithTracks(contentUri);
                observer.onNext(playlist);
                observer.onCompleted();
            }
        }));
    }
}
