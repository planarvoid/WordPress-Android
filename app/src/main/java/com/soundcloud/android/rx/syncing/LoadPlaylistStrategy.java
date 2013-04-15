package com.soundcloud.android.rx.syncing;

import com.soundcloud.android.dao.PlaylistStorage;
import com.soundcloud.android.model.Playlist;
import com.soundcloud.android.utils.UriUtils;
import rx.Observable;
import rx.Observer;
import rx.Subscription;
import rx.subscriptions.Subscriptions;
import rx.util.functions.Func1;

import android.content.Context;
import android.net.Uri;

public class LoadPlaylistStrategy implements SyncOperations.LocalStorageStrategy<Playlist> {

    private PlaylistStorage mStorage;

    public LoadPlaylistStrategy(Context context) {
        mStorage = new PlaylistStorage(context);
    }

    @Override
    public Observable<Playlist> loadFromContentUri(final Uri contentUri) {
        return Observable.create(new Func1<Observer<Playlist>, Subscription>() {
            @Override
            public Subscription call(Observer<Playlist> observer) {
                Playlist playlist = mStorage.getPlaylistWithTracks(UriUtils.getLastSegmentAsLong(contentUri));
                observer.onNext(playlist);
                observer.onCompleted();
                return Subscriptions.empty();
            }
        });
    }
}
