package com.soundcloud.android.rx.schedulers;

import com.soundcloud.android.dao.PlaylistStorage;
import com.soundcloud.android.model.Playlist;
import com.soundcloud.android.rx.observers.DetachableObserver;
import org.jetbrains.annotations.Nullable;
import rx.Observable;

import android.content.Context;
import android.net.Uri;

public class PlaylistsScheduler extends SyncingScheduler<Playlist> {

    private PlaylistStorage mStorage;
    private @Nullable Playlist mPlaylist;

    public PlaylistsScheduler(Context context) {
        super(context);
        mStorage = new PlaylistStorage(context.getContentResolver());
    }

    public Observable<Observable<Playlist>> syncIfNecessary(final Playlist playlist) {
        mPlaylist = playlist;
        return super.syncIfNecessary(playlist.toUri());
    }

    public Observable<Playlist> syncNow(final Playlist playlist) {
        mPlaylist = playlist;
        return super.syncNow(playlist.toUri());
    }

    @Override
    protected Playlist emptyResult() {
        //TODO: use Null objects instead, like Guava's Optional<T>
        return mPlaylist;
    }

    @Override
    public Observable<Playlist> loadFromLocalStorage(final long id) {
        return Observable.create(newBackgroundJob(new ObservedRunnable<Playlist>() {
            @Override
            protected void run(DetachableObserver<Playlist> observer) {
                Playlist playlist = mStorage.getPlaylistWithTracks(id);
                observer.onNext(playlist);
                observer.onCompleted();
            }
        }));
    }

    @Override
    public Observable<Playlist> loadFromLocalStorage(final Uri contentUri) {
        return Observable.create(newBackgroundJob(new ObservedRunnable<Playlist>() {
            @Override
            protected void run(DetachableObserver<Playlist> observer) {
                Playlist playlist = mStorage.getPlaylistWithTracks(contentUri);
                observer.onNext(playlist);
                observer.onCompleted();
            }
        }));
    }
}
