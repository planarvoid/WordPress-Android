package com.soundcloud.android.rx.schedulers;

import com.soundcloud.android.dao.PlaylistStorage;
import com.soundcloud.android.model.Track;
import com.soundcloud.android.utils.UriUtils;
import rx.Observable;
import rx.Observer;
import rx.Subscription;
import rx.subscriptions.Subscriptions;
import rx.util.functions.Func1;

import android.content.Context;
import android.net.Uri;

import java.util.List;


public class LoadPlaylistTracksStrategy implements SyncOperations.LocalStorageStrategy<List<Track>> {

    private PlaylistStorage mStorage;

    public LoadPlaylistTracksStrategy(Context context) {
        mStorage = new PlaylistStorage(context);
    }

    @Override
    public Observable<List<Track>> loadFromContentUri(final Uri contentUri) {
        return Observable.create(new Func1<Observer<List<Track>>, Subscription>() {
            @Override
            public Subscription call(Observer<List<Track>> observer) {
                List<Track> tracks = mStorage.loadPlaylistTracks(UriUtils.getLastSegmentAsLong(contentUri));
                observer.onNext(tracks);
                observer.onCompleted();
                return Subscriptions.empty();
            }
        });
    }

}
